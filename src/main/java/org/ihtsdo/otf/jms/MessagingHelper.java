package org.ihtsdo.otf.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.util.Enumeration;
import java.util.Map;
import javax.jms.*;

/**
 * Provides easy message sending methods while encapsulating OTF JMS strategy with regards
 * to error handling and responding to a request message.
 */
public class MessagingHelper {

	public static final String RESPONSE_POSTFIX = ".response";
	public static final String DEAD_LETTER_QUEUE = "dead-letter-queue";
	public static final String AUTHENTICATION_TOKEN = "authenticationToken";
	public static final String REQUEST_PROPERTY_NAME_PREFIX = "request.";
	public static final String ERROR_FLAG = "error";

	private Logger logger = LoggerFactory.getLogger(getClass());
	public static final MessagePostProcessor postProcessorSetErrorFlag = new MessagePostProcessor() {
		@Override
		public Message postProcessMessage(Message message) throws JMSException {
			message.setBooleanProperty(ERROR_FLAG, true);
			return message;
		}
	};

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ConnectionFactory connectionFactory;

	public void send(String destinationQueueName, Object payload, String responseQueueName) throws JsonProcessingException, JMSException {

	}

	public void send(String destinationQueueName, Object payload, final Map<String, ? extends Object> messageProperties) throws JsonProcessingException, JMSException {
		send(new ActiveMQQueue(destinationQueueName), payload, messageProperties, null);
	}

	public void send(String destinationQueueName, Object payload, final Map<String, ? extends Object> messageProperties, String responseQueueName) throws JsonProcessingException, JMSException {
		send(new ActiveMQQueue(destinationQueueName), payload, messageProperties, new ActiveMQQueue(responseQueueName));
	}

	public void send(String destinationQueueName, Object payload) throws JsonProcessingException, JMSException {
		send(new ActiveMQQueue(destinationQueueName), payload);
	}

	public void sendResponse(final TextMessage incomingMessage, Object payload) {
		sendResponse(incomingMessage, payload, null);
	}

	public void sendResponse(final TextMessage incomingMessage, Object payload, final Map<String, ? extends Object> messageProperties) {
		final Destination replyToDestination = getReplyToDestination(incomingMessage);
		sendWithQueueErrorHandling(replyToDestination, payload, new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws JMSException {
				copyProperties(incomingMessage, message, REQUEST_PROPERTY_NAME_PREFIX);
				setProperties(message, messageProperties);
				return message;
			}
		});
	}

	private void setProperties(Message message, Map<String, ? extends Object> messageProperties) throws JMSException {
		for (Map.Entry<String, ? extends Object> stringObjectEntry : messageProperties.entrySet()) {
			message.setObjectProperty(stringObjectEntry.getKey(), stringObjectEntry.getValue());
		}
	}

	public void send(Destination destination, Object payload) throws JsonProcessingException, JMSException {
		send(destination, payload, null, null);
	}

	public void send(Destination destination, Object payload, final Map<String, ? extends Object> messageProperties, final Destination responseDestination) throws JsonProcessingException, JMSException {
		logger.info("Sending message - destination {}, payload {}, responseDestination {}", getDestinationLabel(destination), payload,
				getDestinationLabel(responseDestination));

		final String message = objectMapper.writeValueAsString(payload);
		getJmsTemplate().convertAndSend(destination, message, new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws JMSException {
				setProperties(message, messageProperties);
				if (responseDestination != null) {
					message.setJMSReplyTo(responseDestination);
				}
				return message;
			}
		});
	}

	/**
	 * This method will not throw an exception if there is a problem serializing the payload, in that scenario the message will be sent to
	 * the original destination with property error=true and details of the error in the payload.
	 *
	 * @param destination
	 * @param payload
	 * @param messagePostProcessor    (optional) Can be null.
	 */
	public void sendWithQueueErrorHandling(Destination destination, Object payload, MessagePostProcessor messagePostProcessor) {
		logger.info("Sending message (headless) - destination {}, payload {}", getDestinationLabel(destination), payload);
		try {
			final String message = objectMapper.writeValueAsString(payload);
			if (messagePostProcessor == null) {
				getJmsTemplate().convertAndSend(destination, message);
			} else {
				getJmsTemplate().convertAndSend(destination, message, messagePostProcessor);
			}
		} catch (JsonProcessingException e) {
			sendError(destination, e);
		}
	}

	public void sendErrorResponse(TextMessage incomingMessage, Exception e) {
		final Destination replyToDestination = getReplyToDestination(incomingMessage);
		sendError(replyToDestination, e);
	}

	/**
	 * The Exception will be serialized as the message payload with a property error=true.
	 *
	 * @param destination
	 * @param e
	 */
	public void sendError(Destination destination, Exception e) {
		logger.info("Sending error - destination {}, payload {}", getDestinationLabel(destination), e);
		String errorMessageString = null;
		try {
			errorMessageString = objectMapper.writeValueAsString(e);
		} catch (JsonProcessingException e1) {
			logger.error("Failed to serialize error {}", e, e1);
			errorMessageString = "Failed to serialize error, see originating application logs for details.";
		}
		getJmsTemplate().convertAndSend(destination, errorMessageString, postProcessorSetErrorFlag);
	}

	public JmsTemplate getJmsTemplate() {
		return new JmsTemplate(connectionFactory);
	}

	public static boolean isError(Message message) throws JMSException {
		return message.getBooleanProperty(ERROR_FLAG);
	}

	/**
	 * Uses message properties to retrieve the reply-to address or builds one based on the name of the original destination.
	 * @param message
	 * @return
	 */
	public static Destination getReplyToDestination(Message message) {
		try {
			Destination replyTo = message.getJMSReplyTo();
			if (replyTo != null) {
				return replyTo;
			} else {
				final Destination jmsDestination = message.getJMSDestination();
				if (jmsDestination instanceof Queue) {
					Queue q = (Queue) jmsDestination;
					final String queueName = q.getQueueName();
					return new ActiveMQQueue(queueName + RESPONSE_POSTFIX);
				} else if (jmsDestination instanceof Topic) {
					Topic topic = (Topic) jmsDestination;
					return new ActiveMQTopic(topic.getTopicName() + RESPONSE_POSTFIX);
				} else {
					throw new java.lang.UnsupportedOperationException("Support of this destination type has not been implemented.");
				}
			}
		} catch(JMSException e) {
			return getDeadLetterQueue();
		}
	}

	public static Destination getDeadLetterQueue() {
		return new ActiveMQQueue(DEAD_LETTER_QUEUE);
	}

	public void copyProperties(Message sourceMessage, Message targetMessage, String newPropertyNamePrefix) throws JMSException {
		@SuppressWarnings("unchecked")
		final Enumeration<String> propertyNames = sourceMessage.getPropertyNames();
		while (propertyNames.hasMoreElements()) {
			final String propertyName = propertyNames.nextElement();
			targetMessage.setObjectProperty(newPropertyNamePrefix + propertyName, sourceMessage.getObjectProperty(propertyName));
		}
	}

	private String getDestinationLabel(Destination destination) {
		if (destination == null) {
			return "null";
		}
		try {
			if (destination instanceof Queue) {
				return "queue " +((Queue) destination).getQueueName();
			} else if (destination instanceof Topic) {
				return "topic " +((Topic) destination).getTopicName();
			} else {
				return "unknown destination type " + destination.toString();
			}
		} catch (JMSException e) {
			logger.error("Failed to retrieve destination details {}", destination, e);
		}
		return "error retrieving destination details";
	}
}

package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

public class Namespace {

	private int namespaceId;
	private String organizationName;
	private String organizationAndContactDetails;
	private String dateIssued;
	private String email;
	private String notes;
	private String idPregenerate;
	private List<Partition> partitions;

	// Getters & setters

	public int getNamespace() { return namespaceId; }
	public void setNamespace(int namespace) { this.namespaceId = namespace; }

	public String getOrganizationName() { return organizationName; }
	public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

	public String getOrganizationAndContactDetails() { return organizationAndContactDetails; }
	public void setOrganizationAndContactDetails(String organizationAndContactDetails) { this.organizationAndContactDetails = organizationAndContactDetails; }

	public String getDateIssued() { return dateIssued; }
	public void setDateIssued(String dateIssued) { this.dateIssued = dateIssued; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getNotes() { return notes; }
	public void setNotes(String notes) { this.notes = notes; }

	public String getIdPregenerate() { return idPregenerate; }
	public void setIdPregenerate(String idPregenerate) { this.idPregenerate = idPregenerate; }

	public List<Partition> getPartitions() { return partitions; }
	public void setPartitions(List<Partition> partitions) { this.partitions = partitions; }

	// Nested class for partition
	public static class Partition {
		private int namespace;
		private String partitionId;
		private long sequence;

		public int getNamespace() { return namespace; }
		public void setNamespace(int namespace) { this.namespace = namespace; }

		public String getPartitionId() { return partitionId; }
		public void setPartitionId(String partitionId) { this.partitionId = partitionId; }

		public long getSequence() { return sequence; }
		public void setSequence(long sequence) { this.sequence = sequence; }
	}
}

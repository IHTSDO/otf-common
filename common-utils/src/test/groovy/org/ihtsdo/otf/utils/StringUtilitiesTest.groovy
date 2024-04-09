package org.ihtsdo.otf.utils

import spock.lang.Specification

class StringUtilitiesTest extends Specification {
    def "Checking multiline string"() {
        when:
            String result = StringUtils.getLineWithMostCharacters(multilineString)

        then:
            result == expectedResult

        where:
            multilineString || expectedResult
            null            || ""
            ""              || ""
            "a\nbb"         || "bb"
            "aa\nb"         || "aa"
            "aa\nbb\ncc"    || "aa"
            "aaa\nbb\ncc"   || "aaa"
            "aa\nbbb\ncc"   || "bbb"
            "aa\nbb\nccc"   || "ccc"
    }
}

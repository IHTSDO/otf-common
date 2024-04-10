package org.snomed.otf.script.dao

import spock.lang.Specification

class ReportSheetManagerTest extends Specification {
    def 'calculateColumnWidth should work'() {
        when:
            def rsm = new ReportSheetManager(null)
            rsm.props = new ReportSheetConfiguration()
            rsm.totalColumnWidthsInCharactersPerTab = [
                    [(double) 120.0, (double) 220.0],
                    [(double) 120.0, (double) 220.0]
            ]
            rsm.totalColumnRowsPerTab = [
                    [1, 10],
                    [2, 20]
            ]

        then:
            expectedWidthInPixels == rsm.calculateColumnWidth(tabIdx, colIdx)

        where:
            tabIdx | colIdx || expectedWidthInPixels
            0      | 0      || 800
            0      | 1      || 209
            1      | 0      || 570
            1      | 1      || 105
    }
}

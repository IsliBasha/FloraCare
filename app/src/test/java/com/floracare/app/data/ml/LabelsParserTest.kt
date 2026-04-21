package com.floracare.app.data.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class LabelsParserTest {

    @Test
    fun `plain one-per-line returns each label verbatim`() {
        val out = parseLabels(
            listOf(
                "Monstera deliciosa",
                "Ficus lyrata",
                "Sansevieria trifasciata",
            ),
        )
        assertEquals(listOf("Monstera deliciosa", "Ficus lyrata", "Sansevieria trifasciata"), out)
    }

    @Test
    fun `blank and comment lines are ignored`() {
        val out = parseLabels(
            listOf(
                "# labels v1",
                "",
                "Monstera deliciosa",
                "   ",
                "Ficus lyrata",
            ),
        )
        assertEquals(listOf("Monstera deliciosa", "Ficus lyrata"), out)
    }

    @Test
    fun `CSV with id+name header takes the last column`() {
        val out = parseLabels(
            listOf(
                "id,name",
                "0,Monstera deliciosa",
                "1,Ficus lyrata",
            ),
        )
        assertEquals(listOf("Monstera deliciosa", "Ficus lyrata"), out)
    }

    @Test
    fun `AIY-style CSV with freebase id is parsed to the common name`() {
        val out = parseLabels(
            listOf(
                "id,mid,name",
                "0,/m/0abc,Monstera deliciosa",
                "1,/m/0def,Ficus lyrata",
            ),
        )
        assertEquals(listOf("Monstera deliciosa", "Ficus lyrata"), out)
    }

    @Test
    fun `TSV is handled as well as CSV`() {
        val out = parseLabels(
            listOf(
                "id\tname",
                "0\tMonstera deliciosa",
                "1\tFicus lyrata",
            ),
        )
        assertEquals(listOf("Monstera deliciosa", "Ficus lyrata"), out)
    }

    @Test
    fun `numeric-only row degrades gracefully to last cell placed at detected index`() {
        // Should not happen in practice. First column "1" is treated as the
        // class index, last cell "3" is taken as the label text.
        val out = parseLabels(listOf("1,2,3"))
        assertEquals(listOf("", "3"), out)
    }

    @Test
    fun `CSV with non-sequential id ordering places each label at its index`() {
        // Mirrors the AIY plants label map: "background" listed at index 2101
        // in the first row, then other classes in natural index order.
        val out = parseLabels(
            listOf(
                "id,name",
                "3,background",
                "0,Monstera deliciosa",
                "1,Ficus lyrata",
                "2,Sansevieria trifasciata",
            ),
        )
        assertEquals(
            listOf("Monstera deliciosa", "Ficus lyrata", "Sansevieria trifasciata", "background"),
            out,
        )
    }

    @Test
    fun `missing ids in the middle yield blank placeholders`() {
        // So `labels[i]` never shifts when a class id is skipped in the file.
        val out = parseLabels(
            listOf(
                "id,name",
                "0,Alpha",
                "2,Gamma",
            ),
        )
        assertEquals(listOf("Alpha", "", "Gamma"), out)
    }
}

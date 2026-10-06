package com.brahma.connect.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSkillCatalogTest {
    @Test
    fun exposesTwoHundredDistinctCategorizedSkills() {
        assertEquals(200, MobileSkillCatalog.all.size)
        assertEquals(200, MobileSkillCatalog.all.map(MobileSkill::id).distinct().size)
        assertTrue(MobileSkillCatalog.categories().containsAll(
            listOf("Coding", "Gaming & performance", "Apps & system", "Files & organization", "Learning & productivity"),
        ))
    }
}

package com.darkrockstudios.apps.c2paverify.model.assertion

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssertionParserTest {

	private val json = Json { ignoreUnknownKeys = true; isLenient = true }

	@Test
	fun `recognises actions assertions`() {
		assertTrue(AssertionParser.isActionsAssertion("c2pa.actions.v2"))
		assertTrue(AssertionParser.isActionsAssertion("c2pa.actions"))
		assertFalse(AssertionParser.isActionsAssertion("stds.schema-org.CreativeWork"))
	}

	@Test
	fun `parses actions with description, software agent, source type and time`() {
		val data = json.parseToJsonElement(
			"""
			{ "actions": [
			  { "action": "c2pa.opened", "description": "Opened by Google" },
			  { "action": "c2pa.edited", "when": "2025-08-07T21:05:49+00:00",
			    "softwareAgent": { "name": "Pixel Camera", "version": "9.1" },
			    "digitalSourceType": "http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia" }
			] }
			""".trimIndent(),
		)
		val actions = AssertionParser.parseActions(data)
		assertEquals(2, actions.size)

		val opened = actions[0]
		assertEquals("c2pa.opened", opened.code)
		assertEquals("Opened", opened.label)
		assertEquals("Opened by Google", opened.description)
		assertNull(opened.digitalSource)

		val edited = actions[1]
		assertEquals("Edited", edited.label)
		assertEquals("Pixel Camera 9.1", edited.softwareAgent)
		assertEquals("2025-08-07T21:05:49+00:00", edited.whenIso)
		assertEquals("Composite including AI", edited.digitalSource?.label)
		assertTrue(edited.digitalSource!!.isAi)
	}

	@Test
	fun `software agent parses a plain string`() {
		val data = json.parseToJsonElement(
			"""{ "actions": [ { "action": "c2pa.created", "softwareAgent": "Adobe Photoshop" } ] }""",
		)
		assertEquals("Adobe Photoshop", AssertionParser.parseActions(data).single().softwareAgent)
	}

	@Test
	fun `non-AI source types are not flagged`() {
		assertFalse(AssertionParser.humanizeSource("http://cv.iptc.org/newscodes/digitalsourcetype/digitalCapture").isAi)
		assertEquals(
			"Digital capture (camera)",
			AssertionParser.humanizeSource("http://cv.iptc.org/newscodes/digitalsourcetype/digitalCapture").label,
		)
		assertFalse(AssertionParser.humanizeSource("http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced").isAi)
	}

	@Test
	fun `unknown action code is prettified`() {
		assertEquals("Color adjustments", AssertionParser.humanizeAction("c2pa.color_adjustments"))
		assertEquals("Some custom thing", AssertionParser.humanizeAction("x.some_custom_thing"))
	}

	@Test
	fun `non-actions data yields no actions`() {
		val data = json.parseToJsonElement("""{ "@type": "CreativeWork" }""")
		assertTrue(AssertionParser.parseActions(data).isEmpty())
	}
}

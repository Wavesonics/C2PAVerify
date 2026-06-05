package com.darkrockstudios.apps.c2paverify.model.assertion

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A single editing/creation step from a `c2pa.actions(.v2)` assertion, in display-ready form. */
data class ParsedAction(
	val code: String,
	/** Human-friendly action name, e.g. "Edited", "Colour adjustments". */
	val label: String,
	val description: String?,
	val softwareAgent: String?,
	val digitalSource: DigitalSource?,
	/** ISO-8601 timestamp from the action's `when`, if present. */
	val whenIso: String?,
)

/** A `digitalSourceType` rendered as a friendly label plus whether it denotes AI generation. */
data class DigitalSource(val label: String, val isAi: Boolean)

/**
 * Pure (KMP-clean) helpers that turn raw C2PA assertion JSON into display-ready structures. Today
 * this richly parses `c2pa.actions(.v2)`; other assertion types fall back to the raw-JSON view in
 * the UI. Trivially unit-testable, no I/O.
 */
object AssertionParser {

	fun isActionsAssertion(label: String): Boolean =
		label == "c2pa.actions" || label == "c2pa.actions.v2"

	/** Extracts the ordered action list from an actions assertion's `data`; empty if none/parse-fail. */
	fun parseActions(data: JsonElement?): List<ParsedAction> {
		val actions = (data as? JsonObject)?.get("actions") as? JsonArray ?: return emptyList()
		return actions.mapNotNull { (it as? JsonObject)?.let(::parseAction) }
	}

	private fun parseAction(obj: JsonObject): ParsedAction {
		val code = obj.string("action") ?: "c2pa.unknown"
		return ParsedAction(
			code = code,
			label = humanizeAction(code),
			description = obj.string("description"),
			softwareAgent = softwareAgent(obj["softwareAgent"]),
			digitalSource = obj.string("digitalSourceType")?.let(::humanizeSource),
			whenIso = obj.string("when"),
		)
	}

	/** A friendly, sentence-case name for a C2PA action code (falls back to the prettified suffix). */
	fun humanizeAction(code: String): String = when (code) {
		"c2pa.created" -> "Created"
		"c2pa.opened" -> "Opened"
		"c2pa.placed" -> "Placed / imported"
		"c2pa.removed" -> "Removed content"
		"c2pa.edited" -> "Edited"
		"c2pa.cropped" -> "Cropped"
		"c2pa.color_adjustments" -> "Color adjustments"
		"c2pa.drawing" -> "Drawing / painting"
		"c2pa.filtered" -> "Filtered"
		"c2pa.resized" -> "Resized"
		"c2pa.orientation" -> "Reoriented"
		"c2pa.transcoded" -> "Transcoded"
		"c2pa.converted" -> "Converted"
		"c2pa.formatted" -> "Reformatted"
		"c2pa.version_updated" -> "Version updated"
		"c2pa.printed" -> "Printed"
		"c2pa.published" -> "Published"
		"c2pa.managed" -> "Managed"
		"c2pa.produced" -> "Produced"
		"c2pa.repackaged" -> "Repackaged"
		"c2pa.redacted" -> "Redacted"
		"c2pa.watermarked" -> "Watermarked"
		"c2pa.dubbed" -> "Dubbed"
		"c2pa.translated" -> "Translated"
		"c2pa.metadata" -> "Metadata edited"
		"c2pa.saved" -> "Saved"
		"c2pa.exported" -> "Exported"
		"c2pa.combined" -> "Combined"
		"c2pa.unknown" -> "Unknown action"
		else -> prettify(code.substringAfterLast('.'))
	}

	/** Maps an IPTC `digitalSourceType` URI to a friendly label and an AI flag. */
	fun humanizeSource(uri: String): DigitalSource {
		val segment = uri.substringAfterLast('/')
		val label = when (segment) {
			"trainedAlgorithmicMedia" -> "Fully AI-generated"
			"compositeWithTrainedAlgorithmicMedia" -> "Composite including AI"
			"algorithmicMedia" -> "Algorithmic media"
			"digitalCapture" -> "Digital capture (camera)"
			"digitalCreation" -> "Digitally created"
			"computationalCapture" -> "Computational capture"
			"algorithmicallyEnhanced" -> "Algorithmically enhanced"
			"dataDrivenMedia" -> "Data-driven media"
			"humanEdits" -> "Human edits"
			"minorHumanEdits" -> "Minor human edits"
			"compositeCapture" -> "Composite capture"
			"composite" -> "Composite"
			"screenCapture" -> "Screen capture"
			"virtualRecording" -> "Virtual recording"
			else -> prettify(segment)
		}
		return DigitalSource(label = label, isAi = segment.lowercase().contains("algorithmicmedia"))
	}

	private fun softwareAgent(element: JsonElement?): String? = when (element) {
		is JsonPrimitive -> if (element.isString) element.content else null
		is JsonObject -> (element["name"] as? JsonPrimitive)?.content?.let { name ->
			val version = (element["version"] as? JsonPrimitive)?.content
			if (version != null) "$name $version" else name
		}

		else -> null
	}

	private fun JsonObject.string(key: String): String? =
		(this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

	/** "color_adjustments" -> "Color adjustments"; "fooBar" -> "Foo bar". */
	private fun prettify(raw: String): String {
		val spaced = raw.replace('_', ' ')
			.replace(Regex("([a-z])([A-Z])"), "$1 $2")
			.trim()
		return spaced.replaceFirstChar { it.uppercase() }.lowercase()
			.replaceFirstChar { it.uppercase() }
	}
}

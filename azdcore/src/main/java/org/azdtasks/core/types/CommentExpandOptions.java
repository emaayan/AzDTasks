package org.azdtasks.core.types;

/*
----------------------------------------------------------
	GENERATED FILE, should be edited to suit the purpose.
----------------------------------------------------------
*/


/**
 * Specifies the additional data retrieval options for work item comments.
**/
public enum CommentExpandOptions {
	none,
	/** Include comment reactions. */
	reactions,
	/** Include the rendered text (html) in addition to MD text. */
	renderedText,
	/** If specified, then ONLY rendered text (html) will be returned, w/o markdown. Supposed to be used internally from data provides for optimization purposes. */
	renderedTextOnly,
	all
}

package uk.co.terminological.fluentxml;

/**
 * Thrown when a DOM node is not an XML element, but an {@link XmlElement}
 * or other element-specific operation is expected.
 *
 * <h2>When thrown</h2>
 * <ul>
 *   <li>{@link XmlElement#fromUnsafe(Xml, Node)} when the node is not an ELEMENT_NODE</li>
 *   <li>Any method that requires an XML element but receives a text, attribute, comment, or other node type</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * try {
 *     Element el = XmlElement.fromUnsafe(xml, textNode);
 * } catch (NotAnElementException e) {
 *     // Handle: got a text node where an element was expected
 * }
 * }</pre>
 *
 * @see XmlElement#fromUnsafe(Xml, Node)
 */
public class NotAnElementException extends RuntimeException {

	/**
	 * Creates a NotAnElementException with a detail message.
	 * @param message the detail message
	 */
	public NotAnElementException(String message) {
		super(message);
	}

	/**
	 * Creates a NotAnElementException with a detail message and cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public NotAnElementException(String message, Throwable cause) {
		super(message, cause);
	}
}

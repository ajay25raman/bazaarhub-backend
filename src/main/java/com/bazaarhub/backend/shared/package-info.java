/**
 * Shared foundation for cross-cutting, generic domain concepts, infrastructure utilities,
 * and presentation contracts.
 *
 * <p>Architectural Rules:
 * <ul>
 *   <li>Shared MUST NOT contain business-specific rules or domain entities.</li>
 *   <li>Avoid using shared as a dumping ground for arbitrary utilities.</li>
 *   <li>Presentation components in shared provide common response wrappers and global exception handling.</li>
 * </ul>
 */
package com.bazaarhub.backend.shared;

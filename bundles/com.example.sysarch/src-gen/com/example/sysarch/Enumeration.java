/**
 */
package com.example.sysarch;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Enumeration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.Enumeration#getLiterals <em>Literals</em>}</li>
 * </ul>
 *
 * @see com.example.sysarch.SysarchPackage#getEnumeration()
 * @model
 * @generated
 */
public interface Enumeration extends Type {
	/**
	 * Returns the value of the '<em><b>Literals</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Literals</em>' attribute list.
	 * @see com.example.sysarch.SysarchPackage#getEnumeration_Literals()
	 * @model
	 * @generated
	 */
	EList<String> getLiterals();

} // Enumeration

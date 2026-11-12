/**
 */
package com.example.sysarch;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Package</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.Package#getSubPackages <em>Sub Packages</em>}</li>
 *   <li>{@link com.example.sysarch.Package#getTypes <em>Types</em>}</li>
 * </ul>
 *
 * @see com.example.sysarch.SysarchPackage#getPackage()
 * @model
 * @generated
 */
public interface Package extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Sub Packages</b></em>' containment reference list.
	 * The list contents are of type {@link com.example.sysarch.Package}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sub Packages</em>' containment reference list.
	 * @see com.example.sysarch.SysarchPackage#getPackage_SubPackages()
	 * @model containment="true"
	 * @generated
	 */
	EList<Package> getSubPackages();

	/**
	 * Returns the value of the '<em><b>Types</b></em>' containment reference list.
	 * The list contents are of type {@link com.example.sysarch.Type}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Types</em>' containment reference list.
	 * @see com.example.sysarch.SysarchPackage#getPackage_Types()
	 * @model containment="true"
	 * @generated
	 */
	EList<Type> getTypes();

} // Package

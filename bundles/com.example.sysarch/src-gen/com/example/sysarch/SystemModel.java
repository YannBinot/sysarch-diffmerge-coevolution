/**
 */
package com.example.sysarch;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>System Model</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.SystemModel#getSourceModelName <em>Source Model Name</em>}</li>
 *   <li>{@link com.example.sysarch.SystemModel#getPackages <em>Packages</em>}</li>
 * </ul>
 *
 * @see com.example.sysarch.SysarchPackage#getSystemModel()
 * @model
 * @generated
 */
public interface SystemModel extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Source Model Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Source Model Name</em>' attribute.
	 * @see #setSourceModelName(String)
	 * @see com.example.sysarch.SysarchPackage#getSystemModel_SourceModelName()
	 * @model
	 * @generated
	 */
	String getSourceModelName();

	/**
	 * Sets the value of the '{@link com.example.sysarch.SystemModel#getSourceModelName <em>Source Model Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Source Model Name</em>' attribute.
	 * @see #getSourceModelName()
	 * @generated
	 */
	void setSourceModelName(String value);

	/**
	 * Returns the value of the '<em><b>Packages</b></em>' containment reference list.
	 * The list contents are of type {@link com.example.sysarch.Package}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Packages</em>' containment reference list.
	 * @see com.example.sysarch.SysarchPackage#getSystemModel_Packages()
	 * @model containment="true"
	 * @generated
	 */
	EList<com.example.sysarch.Package> getPackages();

} // SystemModel

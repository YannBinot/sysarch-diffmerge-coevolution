/**
 */
package com.example.sysarch;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Interface</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.Interface#getOperations <em>Operations</em>}</li>
 * </ul>
 *
 * @see com.example.sysarch.SysarchPackage#getInterface()
 * @model
 * @generated
 */
public interface Interface extends Type {
	/**
	 * Returns the value of the '<em><b>Operations</b></em>' containment reference list.
	 * The list contents are of type {@link com.example.sysarch.Operation}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Operations</em>' containment reference list.
	 * @see com.example.sysarch.SysarchPackage#getInterface_Operations()
	 * @model containment="true"
	 * @generated
	 */
	EList<Operation> getOperations();

} // Interface

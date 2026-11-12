/**
 */
package com.example.sysarch;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Component</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.Component#isAbstract <em>Abstract</em>}</li>
 *   <li>{@link com.example.sysarch.Component#getSuperComponents <em>Super Components</em>}</li>
 *   <li>{@link com.example.sysarch.Component#getProvidedInterfaces <em>Provided Interfaces</em>}</li>
 *   <li>{@link com.example.sysarch.Component#getAttributes <em>Attributes</em>}</li>
 *   <li>{@link com.example.sysarch.Component#getOperations <em>Operations</em>}</li>
 *   <li>{@link com.example.sysarch.Component#getStatus <em>Status</em>}</li>
 *   <li>{@link com.example.sysarch.Component#getNotes <em>Notes</em>}</li>
 * </ul>
 *
 * @see com.example.sysarch.SysarchPackage#getComponent()
 * @model
 * @generated
 */
public interface Component extends Type {
	/**
	 * Returns the value of the '<em><b>Abstract</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Abstract</em>' attribute.
	 * @see #setAbstract(boolean)
	 * @see com.example.sysarch.SysarchPackage#getComponent_Abstract()
	 * @model
	 * @generated
	 */
	boolean isAbstract();

	/**
	 * Sets the value of the '{@link com.example.sysarch.Component#isAbstract <em>Abstract</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Abstract</em>' attribute.
	 * @see #isAbstract()
	 * @generated
	 */
	void setAbstract(boolean value);

	/**
	 * Returns the value of the '<em><b>Super Components</b></em>' reference list.
	 * The list contents are of type {@link com.example.sysarch.Component}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Super Components</em>' reference list.
	 * @see com.example.sysarch.SysarchPackage#getComponent_SuperComponents()
	 * @model
	 * @generated
	 */
	EList<Component> getSuperComponents();

	/**
	 * Returns the value of the '<em><b>Provided Interfaces</b></em>' reference list.
	 * The list contents are of type {@link com.example.sysarch.Interface}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Provided Interfaces</em>' reference list.
	 * @see com.example.sysarch.SysarchPackage#getComponent_ProvidedInterfaces()
	 * @model
	 * @generated
	 */
	EList<Interface> getProvidedInterfaces();

	/**
	 * Returns the value of the '<em><b>Attributes</b></em>' containment reference list.
	 * The list contents are of type {@link com.example.sysarch.Attribute}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Attributes</em>' containment reference list.
	 * @see com.example.sysarch.SysarchPackage#getComponent_Attributes()
	 * @model containment="true"
	 * @generated
	 */
	EList<Attribute> getAttributes();

	/**
	 * Returns the value of the '<em><b>Operations</b></em>' containment reference list.
	 * The list contents are of type {@link com.example.sysarch.Operation}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Operations</em>' containment reference list.
	 * @see com.example.sysarch.SysarchPackage#getComponent_Operations()
	 * @model containment="true"
	 * @generated
	 */
	EList<Operation> getOperations();

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * The default value is <code>"DRAFT"</code>.
	 * The literals are from the enumeration {@link com.example.sysarch.ComponentStatus}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Project-owned information: never imported from the external model.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see com.example.sysarch.ComponentStatus
	 * @see #setStatus(ComponentStatus)
	 * @see com.example.sysarch.SysarchPackage#getComponent_Status()
	 * @model default="DRAFT"
	 * @generated
	 */
	ComponentStatus getStatus();

	/**
	 * Sets the value of the '{@link com.example.sysarch.Component#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see com.example.sysarch.ComponentStatus
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(ComponentStatus value);

	/**
	 * Returns the value of the '<em><b>Notes</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Project-owned information: never imported from the external model.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Notes</em>' attribute.
	 * @see #setNotes(String)
	 * @see com.example.sysarch.SysarchPackage#getComponent_Notes()
	 * @model
	 * @generated
	 */
	String getNotes();

	/**
	 * Sets the value of the '{@link com.example.sysarch.Component#getNotes <em>Notes</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Notes</em>' attribute.
	 * @see #getNotes()
	 * @generated
	 */
	void setNotes(String value);

} // Component

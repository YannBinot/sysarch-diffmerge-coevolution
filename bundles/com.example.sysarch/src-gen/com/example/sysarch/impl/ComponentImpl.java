/**
 */
package com.example.sysarch.impl;

import com.example.sysarch.Attribute;
import com.example.sysarch.Component;
import com.example.sysarch.ComponentStatus;
import com.example.sysarch.Interface;
import com.example.sysarch.Operation;
import com.example.sysarch.SysarchPackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Component</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#isAbstract <em>Abstract</em>}</li>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#getSuperComponents <em>Super Components</em>}</li>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#getProvidedInterfaces <em>Provided Interfaces</em>}</li>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#getAttributes <em>Attributes</em>}</li>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#getOperations <em>Operations</em>}</li>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#getStatus <em>Status</em>}</li>
 *   <li>{@link com.example.sysarch.impl.ComponentImpl#getNotes <em>Notes</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ComponentImpl extends TypeImpl implements Component {
	/**
	 * The default value of the '{@link #isAbstract() <em>Abstract</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAbstract()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ABSTRACT_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isAbstract() <em>Abstract</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAbstract()
	 * @generated
	 * @ordered
	 */
	protected boolean abstract_ = ABSTRACT_EDEFAULT;

	/**
	 * The cached value of the '{@link #getSuperComponents() <em>Super Components</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSuperComponents()
	 * @generated
	 * @ordered
	 */
	protected EList<Component> superComponents;

	/**
	 * The cached value of the '{@link #getProvidedInterfaces() <em>Provided Interfaces</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProvidedInterfaces()
	 * @generated
	 * @ordered
	 */
	protected EList<Interface> providedInterfaces;

	/**
	 * The cached value of the '{@link #getAttributes() <em>Attributes</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAttributes()
	 * @generated
	 * @ordered
	 */
	protected EList<Attribute> attributes;

	/**
	 * The cached value of the '{@link #getOperations() <em>Operations</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperations()
	 * @generated
	 * @ordered
	 */
	protected EList<Operation> operations;

	/**
	 * The default value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected static final ComponentStatus STATUS_EDEFAULT = ComponentStatus.DRAFT;

	/**
	 * The cached value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected ComponentStatus status = STATUS_EDEFAULT;

	/**
	 * The default value of the '{@link #getNotes() <em>Notes</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNotes()
	 * @generated
	 * @ordered
	 */
	protected static final String NOTES_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getNotes() <em>Notes</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNotes()
	 * @generated
	 * @ordered
	 */
	protected String notes = NOTES_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ComponentImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return SysarchPackage.Literals.COMPONENT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAbstract() {
		return abstract_;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAbstract(boolean newAbstract) {
		boolean oldAbstract = abstract_;
		abstract_ = newAbstract;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SysarchPackage.COMPONENT__ABSTRACT, oldAbstract, abstract_));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Component> getSuperComponents() {
		if (superComponents == null) {
			superComponents = new EObjectResolvingEList<Component>(Component.class, this, SysarchPackage.COMPONENT__SUPER_COMPONENTS);
		}
		return superComponents;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Interface> getProvidedInterfaces() {
		if (providedInterfaces == null) {
			providedInterfaces = new EObjectResolvingEList<Interface>(Interface.class, this, SysarchPackage.COMPONENT__PROVIDED_INTERFACES);
		}
		return providedInterfaces;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Attribute> getAttributes() {
		if (attributes == null) {
			attributes = new EObjectContainmentEList<Attribute>(Attribute.class, this, SysarchPackage.COMPONENT__ATTRIBUTES);
		}
		return attributes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Operation> getOperations() {
		if (operations == null) {
			operations = new EObjectContainmentEList<Operation>(Operation.class, this, SysarchPackage.COMPONENT__OPERATIONS);
		}
		return operations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentStatus getStatus() {
		return status;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStatus(ComponentStatus newStatus) {
		ComponentStatus oldStatus = status;
		status = newStatus == null ? STATUS_EDEFAULT : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SysarchPackage.COMPONENT__STATUS, oldStatus, status));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getNotes() {
		return notes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setNotes(String newNotes) {
		String oldNotes = notes;
		notes = newNotes;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SysarchPackage.COMPONENT__NOTES, oldNotes, notes));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case SysarchPackage.COMPONENT__ATTRIBUTES:
				return ((InternalEList<?>)getAttributes()).basicRemove(otherEnd, msgs);
			case SysarchPackage.COMPONENT__OPERATIONS:
				return ((InternalEList<?>)getOperations()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case SysarchPackage.COMPONENT__ABSTRACT:
				return isAbstract();
			case SysarchPackage.COMPONENT__SUPER_COMPONENTS:
				return getSuperComponents();
			case SysarchPackage.COMPONENT__PROVIDED_INTERFACES:
				return getProvidedInterfaces();
			case SysarchPackage.COMPONENT__ATTRIBUTES:
				return getAttributes();
			case SysarchPackage.COMPONENT__OPERATIONS:
				return getOperations();
			case SysarchPackage.COMPONENT__STATUS:
				return getStatus();
			case SysarchPackage.COMPONENT__NOTES:
				return getNotes();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case SysarchPackage.COMPONENT__ABSTRACT:
				setAbstract((Boolean)newValue);
				return;
			case SysarchPackage.COMPONENT__SUPER_COMPONENTS:
				getSuperComponents().clear();
				getSuperComponents().addAll((Collection<? extends Component>)newValue);
				return;
			case SysarchPackage.COMPONENT__PROVIDED_INTERFACES:
				getProvidedInterfaces().clear();
				getProvidedInterfaces().addAll((Collection<? extends Interface>)newValue);
				return;
			case SysarchPackage.COMPONENT__ATTRIBUTES:
				getAttributes().clear();
				getAttributes().addAll((Collection<? extends Attribute>)newValue);
				return;
			case SysarchPackage.COMPONENT__OPERATIONS:
				getOperations().clear();
				getOperations().addAll((Collection<? extends Operation>)newValue);
				return;
			case SysarchPackage.COMPONENT__STATUS:
				setStatus((ComponentStatus)newValue);
				return;
			case SysarchPackage.COMPONENT__NOTES:
				setNotes((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case SysarchPackage.COMPONENT__ABSTRACT:
				setAbstract(ABSTRACT_EDEFAULT);
				return;
			case SysarchPackage.COMPONENT__SUPER_COMPONENTS:
				getSuperComponents().clear();
				return;
			case SysarchPackage.COMPONENT__PROVIDED_INTERFACES:
				getProvidedInterfaces().clear();
				return;
			case SysarchPackage.COMPONENT__ATTRIBUTES:
				getAttributes().clear();
				return;
			case SysarchPackage.COMPONENT__OPERATIONS:
				getOperations().clear();
				return;
			case SysarchPackage.COMPONENT__STATUS:
				setStatus(STATUS_EDEFAULT);
				return;
			case SysarchPackage.COMPONENT__NOTES:
				setNotes(NOTES_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case SysarchPackage.COMPONENT__ABSTRACT:
				return abstract_ != ABSTRACT_EDEFAULT;
			case SysarchPackage.COMPONENT__SUPER_COMPONENTS:
				return superComponents != null && !superComponents.isEmpty();
			case SysarchPackage.COMPONENT__PROVIDED_INTERFACES:
				return providedInterfaces != null && !providedInterfaces.isEmpty();
			case SysarchPackage.COMPONENT__ATTRIBUTES:
				return attributes != null && !attributes.isEmpty();
			case SysarchPackage.COMPONENT__OPERATIONS:
				return operations != null && !operations.isEmpty();
			case SysarchPackage.COMPONENT__STATUS:
				return status != STATUS_EDEFAULT;
			case SysarchPackage.COMPONENT__NOTES:
				return NOTES_EDEFAULT == null ? notes != null : !NOTES_EDEFAULT.equals(notes);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (abstract: ");
		result.append(abstract_);
		result.append(", status: ");
		result.append(status);
		result.append(", notes: ");
		result.append(notes);
		result.append(')');
		return result.toString();
	}

} //ComponentImpl

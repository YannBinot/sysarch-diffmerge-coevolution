/**
 */
package com.example.sysarch.impl;

import com.example.sysarch.SysarchPackage;
import com.example.sysarch.SystemModel;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>System Model</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.impl.SystemModelImpl#getSourceModelName <em>Source Model Name</em>}</li>
 *   <li>{@link com.example.sysarch.impl.SystemModelImpl#getPackages <em>Packages</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SystemModelImpl extends NamedElementImpl implements SystemModel {
	/**
	 * The default value of the '{@link #getSourceModelName() <em>Source Model Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSourceModelName()
	 * @generated
	 * @ordered
	 */
	protected static final String SOURCE_MODEL_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSourceModelName() <em>Source Model Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSourceModelName()
	 * @generated
	 * @ordered
	 */
	protected String sourceModelName = SOURCE_MODEL_NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #getPackages() <em>Packages</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPackages()
	 * @generated
	 * @ordered
	 */
	protected EList<com.example.sysarch.Package> packages;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SystemModelImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return SysarchPackage.Literals.SYSTEM_MODEL;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSourceModelName() {
		return sourceModelName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSourceModelName(String newSourceModelName) {
		String oldSourceModelName = sourceModelName;
		sourceModelName = newSourceModelName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SysarchPackage.SYSTEM_MODEL__SOURCE_MODEL_NAME, oldSourceModelName, sourceModelName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<com.example.sysarch.Package> getPackages() {
		if (packages == null) {
			packages = new EObjectContainmentEList<com.example.sysarch.Package>(com.example.sysarch.Package.class, this, SysarchPackage.SYSTEM_MODEL__PACKAGES);
		}
		return packages;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case SysarchPackage.SYSTEM_MODEL__PACKAGES:
				return ((InternalEList<?>)getPackages()).basicRemove(otherEnd, msgs);
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
			case SysarchPackage.SYSTEM_MODEL__SOURCE_MODEL_NAME:
				return getSourceModelName();
			case SysarchPackage.SYSTEM_MODEL__PACKAGES:
				return getPackages();
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
			case SysarchPackage.SYSTEM_MODEL__SOURCE_MODEL_NAME:
				setSourceModelName((String)newValue);
				return;
			case SysarchPackage.SYSTEM_MODEL__PACKAGES:
				getPackages().clear();
				getPackages().addAll((Collection<? extends com.example.sysarch.Package>)newValue);
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
			case SysarchPackage.SYSTEM_MODEL__SOURCE_MODEL_NAME:
				setSourceModelName(SOURCE_MODEL_NAME_EDEFAULT);
				return;
			case SysarchPackage.SYSTEM_MODEL__PACKAGES:
				getPackages().clear();
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
			case SysarchPackage.SYSTEM_MODEL__SOURCE_MODEL_NAME:
				return SOURCE_MODEL_NAME_EDEFAULT == null ? sourceModelName != null : !SOURCE_MODEL_NAME_EDEFAULT.equals(sourceModelName);
			case SysarchPackage.SYSTEM_MODEL__PACKAGES:
				return packages != null && !packages.isEmpty();
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
		result.append(" (sourceModelName: ");
		result.append(sourceModelName);
		result.append(')');
		return result.toString();
	}

} //SystemModelImpl

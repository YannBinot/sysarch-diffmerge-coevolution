/**
 */
package com.example.sysarch.impl;

import com.example.sysarch.SysarchPackage;
import com.example.sysarch.Type;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Package</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link com.example.sysarch.impl.PackageImpl#getSubPackages <em>Sub Packages</em>}</li>
 *   <li>{@link com.example.sysarch.impl.PackageImpl#getTypes <em>Types</em>}</li>
 * </ul>
 *
 * @generated
 */
public class PackageImpl extends NamedElementImpl implements com.example.sysarch.Package {
	/**
	 * The cached value of the '{@link #getSubPackages() <em>Sub Packages</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSubPackages()
	 * @generated
	 * @ordered
	 */
	protected EList<com.example.sysarch.Package> subPackages;

	/**
	 * The cached value of the '{@link #getTypes() <em>Types</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypes()
	 * @generated
	 * @ordered
	 */
	protected EList<Type> types;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected PackageImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return SysarchPackage.Literals.PACKAGE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<com.example.sysarch.Package> getSubPackages() {
		if (subPackages == null) {
			subPackages = new EObjectContainmentEList<com.example.sysarch.Package>(com.example.sysarch.Package.class, this, SysarchPackage.PACKAGE__SUB_PACKAGES);
		}
		return subPackages;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Type> getTypes() {
		if (types == null) {
			types = new EObjectContainmentEList<Type>(Type.class, this, SysarchPackage.PACKAGE__TYPES);
		}
		return types;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case SysarchPackage.PACKAGE__SUB_PACKAGES:
				return ((InternalEList<?>)getSubPackages()).basicRemove(otherEnd, msgs);
			case SysarchPackage.PACKAGE__TYPES:
				return ((InternalEList<?>)getTypes()).basicRemove(otherEnd, msgs);
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
			case SysarchPackage.PACKAGE__SUB_PACKAGES:
				return getSubPackages();
			case SysarchPackage.PACKAGE__TYPES:
				return getTypes();
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
			case SysarchPackage.PACKAGE__SUB_PACKAGES:
				getSubPackages().clear();
				getSubPackages().addAll((Collection<? extends com.example.sysarch.Package>)newValue);
				return;
			case SysarchPackage.PACKAGE__TYPES:
				getTypes().clear();
				getTypes().addAll((Collection<? extends Type>)newValue);
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
			case SysarchPackage.PACKAGE__SUB_PACKAGES:
				getSubPackages().clear();
				return;
			case SysarchPackage.PACKAGE__TYPES:
				getTypes().clear();
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
			case SysarchPackage.PACKAGE__SUB_PACKAGES:
				return subPackages != null && !subPackages.isEmpty();
			case SysarchPackage.PACKAGE__TYPES:
				return types != null && !types.isEmpty();
		}
		return super.eIsSet(featureID);
	}

} //PackageImpl

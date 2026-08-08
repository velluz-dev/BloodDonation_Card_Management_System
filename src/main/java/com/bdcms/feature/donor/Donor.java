package com.bdcms.feature.donor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * A registered blood donor (FR-2) - the single, centralised record for one
 * person, keyed by a nationally unique identification number.
 *
 * <p>{@code pinHash} holds the optional four-digit PIN described in Section 10
 * of the proposal. Like a staff password it is BCrypt hashed, and it is never
 * rendered to a page.
 */
public class Donor {

    private long donorId;
    private String nationalId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private BloodGroup bloodGroup;
    private String contactNo;
    private String address;
    private String photoPath;
    private String pinHash;
    private LocalDateTime registeredOn;

    public long getDonorId() {
        return donorId;
    }

    public void setDonorId(long donorId) {
        this.donorId = donorId;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public LocalDateTime getRegisteredOn() {
        return registeredOn;
    }

    public void setRegisteredOn(LocalDateTime registeredOn) {
        this.registeredOn = registeredOn;
    }

    // ---- derived, for the views -------------------------------------------

    /** True when a PIN was set at registration, so the verification screen can offer it. */
    public boolean isPinSet() {
        return pinHash != null && !pinHash.isBlank();
    }

    public boolean isHasPhoto() {
        return photoPath != null && !photoPath.isBlank();
    }

    public int getAge() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    // ---- pre-formatted for the views (JSTL cannot format java.time) --------

    public String getDateOfBirthDisplay() {
        return com.bdcms.shared.util.Dates.display(dateOfBirth);
    }

    public String getRegisteredOnDisplay() {
        return com.bdcms.shared.util.Dates.display(registeredOn);
    }

    /** ISO form, for populating an {@code <input type="date">}. */
    public String getDateOfBirthIso() {
        return dateOfBirth == null ? "" : dateOfBirth.toString();
    }
}

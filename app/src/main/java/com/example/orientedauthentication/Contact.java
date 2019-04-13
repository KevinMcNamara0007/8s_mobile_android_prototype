package com.example.orientedauthentication;

import java.util.ArrayList;

/**
 * Contact class is a model class to represent a contact and aimed
 * to be used with a recycler view adapter.
 */
public class Contact {
    private String mName;
    private boolean mOnline;

    public Contact(String name, boolean online) {
        mName = name;
        mOnline = online;
    }

    public String getName() {
        return mName;
    }

    public boolean isOnline() {
        return mOnline;
    }

    private static int lastContactId = 0;

    /**
     * Creates a dummy array list of dummy contacts
     *
     * @param numContacts how many contacts should be in the array list
     * @return an arraylist of dummy contacts
     */
    public static ArrayList<Contact> createContactsList(int numContacts) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();

        for (int i = 1; i <= numContacts; i++) {
            contacts.add(new Contact("Person " + ++lastContactId, i <= numContacts / 2));
        }

        return contacts;
    }
}
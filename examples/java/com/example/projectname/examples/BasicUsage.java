package com.example.projectname.examples;

import com.example.projectname.internal.StringUtils;

public class BasicUsage {
    public static void main(String[] args) {
        System.out.println("Demonstrating StringUtils from the main project:");

        String text = "Hello Example";
        System.out.println("Original: " + text);
        System.out.println("Reversed: " + StringUtils.reverse(text));

        String emptyText = "";
        System.out.println("Is '" + emptyText + "' null or empty? " + StringUtils.isNullOrEmpty(emptyText));

        String nullText = null;
        System.out.println("Is nullText null or empty? " + StringUtils.isNullOrEmpty(nullText));
    }
}

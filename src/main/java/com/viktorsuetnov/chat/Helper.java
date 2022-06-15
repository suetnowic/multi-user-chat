package com.viktorsuetnov.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Helper {

    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static String readString() {
        String string;
        try {
            string = reader.readLine();
        } catch (IOException e) {
            System.out.println("Oops, an error occurred, please try again.");
            string = readString();
        }
        return string;
    }

    public static Integer readInt() {
        Integer number;
        try {
            number = Integer.parseInt(reader.readLine());
        } catch (IOException e) {
            System.out.println("Oops, an error occurred, please try again.");
            number = readInt();
        }
        return number;
    }

    public static void showMessage(String message) {
        System.out.println(message);
    }
}

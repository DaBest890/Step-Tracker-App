//Maximo Obra Winfield
//IT 145
// Professor Sellers
// 10/12/2024



package Module6Assignment;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Paint1 {

    public static void main(String[] args) {
        Scanner scnr = new Scanner(System.in);
        double wallHeight = 0.0;
        double wallWidth = 0.0;
        double wallArea = 0.0;
        double gallonsPaintNeeded = 0.0;
        
        final double squareFeetPerGallon = 350.0;
        boolean validInput = false;

        // Loop for wall height input
        do {
            try {
                System.out.println("Enter wall height (feet): ");
                wallHeight = scnr.nextDouble();
                
                if (wallHeight > 0) {
                    validInput = true; // Valid input received
                } else {
                    System.out.println("Error: Wall height must be greater than 0.");
                    validInput = false;
                }
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input. Please enter a number.");
                scnr.next(); // Clear invalid input
                validInput = false;
            }
        } while (!validInput);
        
        // Reset validation for next input
        validInput = false;

        // Loop for wall width input
        do {
            try {
                System.out.println("Enter wall width (feet): ");
                wallWidth = scnr.nextDouble();
                
                if (wallWidth > 0) {
                    validInput = true; // Valid input received
                } else {
                    System.out.println("Error: Wall width must be greater than 0.");
                    validInput = false;
                }
            } catch (InputMismatchException e) {
                System.out.println("Error: Invalid input. Please enter a number.");
                scnr.next(); // Clear invalid input
                validInput = false;
            }
        } while (!validInput);

        // Calculate and output wall area
        wallArea = wallHeight * wallWidth;
        System.out.println("Wall area: " + wallArea + " square feet");

        // Calculate and output the amount of paint (in gallons) needed to paint the wall
        gallonsPaintNeeded = wallArea / squareFeetPerGallon;
        System.out.println("Paint needed: " + gallonsPaintNeeded + " gallons");

        scnr.close(); // Close the scanner
    }
}

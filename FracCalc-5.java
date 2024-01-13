// Tanisha Thakare
// Fraction Calculator Project

import java.util.*;

// This program can do a number of mathematical operations with whole numbers, fractions
//and mixed numbers. You can add, subtract, multiply, and divide fractions!
//To type in a fraction use / and to type a mixed number use _ in between the whole number and the numerator!
//for example 3/4 + 5_3/4

public class FracCalc {

   // It is best if we have only one console object for input
   public static Scanner console = new Scanner(System.in);
   
   public static void main(String[] args) {
   
      boolean done = false;
      
      while (!done) {
         
         String input = getInput();
         
         
         if (input.equalsIgnoreCase("quit")) {
            done = true;
         } else if (!UnitTestRunner.processCommand(input, FracCalc::processCommand)) {
        	   
            String result = processCommand(input);
            
            
            System.out.println(result);
         }
      }
      
      System.out.println("Goodbye!");
      console.close();
   }

   
   public static String getInput() {
      System.out.printf("Enter: ");
      String response = console.nextLine();
      return response;
   }
  
   public static String processCommand(String input) {
      if (input.equalsIgnoreCase("help")) {
         return provideHelp();
      }
      
      return processExpression(input);
   }
   
   public static String processExpression(String input) {
    
      Scanner parser = new Scanner(input);
      
      //Gets the Tokens
      String token1 = parser.next(); 
      String operator = parser.next();
      String token3 = parser.next();
      
      
      int numerator1 = getNumerator(token1);
      int denominator1 = getDenominator(token1);
      int whole1 = getWhole(token1);
      
      int numerator2 = getNumerator(token3);
      int denominator2 = getDenominator(token3);
      int whole2 = getWhole(token3);
      
      if (whole1 < 0) {
         numerator1 *= -1; 
      }
      
      if (whole2 < 0) {
         numerator2 *= -1;
      }
         
      // gets the improper fractions
      int n1 = denominator1 * whole1 + numerator1;
      int n2 = denominator2 * whole2 + numerator2;
      
      //Operator is converted to a String.
      String operation = "";
      //When the operator is the star(*), it calls the multiplication method.
      if (operator.equals("*")) {
         return multiplication(n1, n2, denominator1, denominator2);
      // When the operator is a slash (/) , it calls the division method.
      } else if (operator.equals("/")) {
         return division(n1, n2, denominator1, denominator2);
      // When the operator is a minus sign (-), n2 changes into a negative n2.
      } else if(operator.equals("-")) {
         n2 = -1 * n2;
      }
    
      int num = denominator1 * n2 + denominator2 * n1;
      int den = denominator1 * denominator2;
      
      //Converts the Integers to a String.
      String n = Integer.toString(num);
      String d = Integer.toString(den);
      
      return simplify(num, den);
   }

//This method is responsible for doing the division of an expression.
   public static String division(int num1, int num2, int den1, int den2) {
      String answer = "";
      int AnswerNum1 = 0;
      int AnswerNum2 = 0;
      int AnswerDen1 = 0;
      int AnswerDen2 = 0;
      
      AnswerNum1 = num1 * den2;
      AnswerDen1 = den1 * num2;
      
      answer = AnswerNum1 + "/" + AnswerDen1;
      return simplify(AnswerNum1, AnswerDen1);
   }
   
   //This method is responsible for doing the multiplication of an expression.
   public static String multiplication(int num1, int num2, int den1, int den2) {     
   
      String answer = "";
      
      //Multiplies the numerator and the denominators 
      //together to get the new nume and den.
      //new den meaning the common denominators.
      int num = num1 * num2;
      int den = den1 * den2;
      
      //For example 3/3 simplifies to 1.
      if (num == den) {
         return "1";
      }
      
      //if the numerator is 0, for example 3_0/1:
      //There is no fraction attached to the whole number
      //so it just becomes a whole number.
      if (num == 0) {
         return simplify(num, den);
      }
      
      //If the numerator is greater than the denominator, it is a improper fraction.
      if (num > den) {
         String reduce = simplify(num, den);
         return reduce;  
      }
      answer = num + "/" + den;
      return answer;
   }
   //This method is responsible for simplifying the negative signs.
   //For example, if the num and den both have a negative sign, it becomes positive.
   public static String SimplifyNegative(String num, String den) {
      //if num and den are negative, change to positive.
      if (num.contains("-") && den.contains("-")) {
         num = num.substring(1);
         //removes negative sign
         den = den.substring(1);
      //if the denominator has a negative, the negative gets
      //removed from the denominator and moves to the numerator.
      } else if (den.contains("-")) {
         num = "-" + num;
         //removes the negative
         den = den.substring(1); 
      } 
      return num + "/" + den;
   }
     
   //This method is responsible for simplifying fractions.
   //e.g. 4/8 ---> 1/2
   public static String simplify(int num, int den) {
      // reduce
      int GCD = gcd(num, den);
      num /= GCD;
      den /= GCD;
      // format
      int whole = num / den;
      num = num % den;
      
      if (num == 0) {
         return whole + "";         
      }
      //if whole is equal to zero, they get converted into a String
      //and then calls the SimplifyNegative method with n and d parameters.   
      if (whole == 0) {
         String n = Integer.toString(num);
         String d = Integer.toString(den);
         String w = Integer.toString(whole);
         return SimplifyNegative(n, d); 
      }
      //if whole is not equal to zero
      if (whole != 0) {
         String n = Integer.toString(num);
         String d = Integer.toString(den);
         String w = Integer.toString(whole);
         //if num and whole has a negative, the negative is removed from the numerator.
         if (n.contains("-") && w.contains("-")) {
            String numerator = n.substring(1);
            return w + " " + numerator + "/" + d;
         //if the den and whole have a negative, the negative sign is removed from the den.
         } else if (d.contains("-") && w.contains("-")) {
            String denominator = d.substring(1);
            return w + " " + n + "/" + denominator;
         }
      }
      return whole + " " + num + "/" + den;
   } 
   
   //This method is responsible for finding the GCD of 2 numbers.
   public static int gcd(int num, int den) {
      while (den != 0) {
         int value = num % den;
         num = den;
         den = value;
      }
      int i = Math.abs(num);
      return i;
   }
   //This method gets the numerator
   public static int getNumerator(String num) {
      if (num.contains("/") && num.contains("_")) {
         String hold = num.substring(num.indexOf("_") + 1 , num.indexOf("/"));
         return Integer.parseInt(hold);
      } 
      else if(num.contains("/") && !num.contains("_")) {
         return Integer.parseInt(num.substring(0, num.indexOf("/")));
      }
      return 0;     
   }
     
   //This method gets the whole number. 
   public static int getWhole(String num) {
      if (!num.contains("/") && !num.contains("_")) {
         return Integer.parseInt(num);
      }
      else if (num.contains("_")) {
         return Integer.parseInt(num.substring(0, num.indexOf("_")));
      }
      return 0;
   }
   
   //This method gets denominator  
   public static int getDenominator(String num) {
      //if the num contains a slash, it is a fraction.
      if (num.contains("/")) {
         return Integer.parseInt(num.substring(num.indexOf("/") + 1));
      }
      //if the num doesn't contain the slash, the denominator will always be 1
      //because if the denominator is zero, that means it's undefined.
      return 1;
   }

   // Returns a string that is helpful to the user about how
   // to use the program. These are instructions to the user.
   public static String provideHelp() {
      
      String help = "Hi! Welcome to the Fraction Calculator.\n";
      help += "You can add, subtract, multiply, and divide fractions!\nTo type in a fraction use / "; 
      help += "and to type a mixed number use _ in between the whole number and the numerator!\nHave Fun!!!";
      
      return help;
   }
}
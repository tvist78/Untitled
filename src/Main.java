import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class Main {
    private static void PrintArray(ArrayList<ArrayList<String>> arr){

        for (int i = 0; i<arr.size(); i++) {

            System.out.println(i+"_"+arr.get(i).get(0)+" "+i+"_"+arr.get(i).get(1));
        }
    }

    public static void main(String[] args) {
        /*ArrayList<ArrayList<String>> arrL = new ArrayList<ArrayList<String>>();
//        arrL.add((ArrayList<String>) Arrays.asList("one","two"));
        for (int i =0; i < 20; i++)
        arrL.add(new ArrayList<String>(Arrays.asList("one", "two")));

        PrintArray(arrL);

    }
    /*public static void main(String[] args) throws ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.mm.yyyy");
        Date date =  sdf.parse("01.06.2016");

//        java.util.Date utilDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        System.out.println("utilDate:" + date);
        System.out.println("sqlDate:" + sqlDate);
*/
        String str = "12345/1";
        int i = Integer.parseInt(str.substring(0,5));
        System.out.println(i);
        String str1;
        int index = str.indexOf('/');

        if (index == -1) str1 = "1";
        else str1 = str.substring(index+1,str.length());
        System.out.println(new  StringBuffer(str).delete(2,5));

    }
}

package org.hotteam67.firebaseviewer.data;

import android.support.annotation.NonNull;

import org.hotteam67.common.FileHandler;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.web.FireBaseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * The Viewer schema, should be updated to match the schema created in the Server app (or written manually for the brave)
 *
 * Need to update all three - sumcolumns, calculated columns, and preferred order. FIRST do sumc olumns, THEN preferred order,
 * THEN calculated columns
 *
 * A note about "raw names" - they are either the name as named in SumColumns, or the name as named in the schema.
 * If the raw name is from the schema it has the most recent header first, such as "Teleop Hatch Panels".
 * If the raw name is from SumColumns(), you obviously need to add a SumColumns() entry, then you can use calculated columns on that
 */
public class ColumnSchema {

    /**
     * The calculated columns desired names, first argument is name as it appears in the viewer, second is the raw name
     * @return the calculated columns to show in the main view
     */
    public static List<CalculatedColumn> CalculatedColumns() {
        List<CalculatedColumn> calculatedColumns = new ArrayList<>();

        String schemaRaw = FileHandler.LoadContents(FileHandler.Files.SCHEMA_CACHE_FILE);

        if (schemaRaw.isEmpty()) return calculatedColumns;
        try {
            JSONObject schema = new JSONObject(schemaRaw);
            JSONArray columns = schema.getJSONArray("calculatedColumns");
            for (int i = 0; i < columns.length(); i++) {
                if (columns.isNull(i)) continue;
                JSONObject column = columns.getJSONObject(i);
                calculatedColumns.add(new CalculatedColumn(column.getString("name"),
                                column.getString("rawName")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return calculatedColumns;
        }

//        calculatedColumns.add(new CalculatedColumn("To. Cargo", "Total Cargo")); //sum upper+lower
//        calculatedColumns.add(new CalculatedColumn("To. U. Cargo", "Total Upper Cargo")); //sum upper auton+teleop
//        calculatedColumns.add(new CalculatedColumn("To. L. Cargo", "Total Lower Cargo")); //sum lower auton+teleop
//        calculatedColumns.add(new CalculatedColumn("Starting Position", "Auton Starting Position")); //Selection
//        calculatedColumns.add(new CalculatedColumn("Cross the Line", "Auton Cross The Line")); //Boolean
//        calculatedColumns.add(new CalculatedColumn("A. U. Cargo", "Auton Upper Cargo")); //Number
//        calculatedColumns.add(new CalculatedColumn("A. L. Cargo", "Auton Lower Cargo")); //Number
//        calculatedColumns.add(new CalculatedColumn("Tel. U. Cargo", "Teleop Upper Cargo")); //Number
////        calculatedColumns.add(new CalculatedColumn("Tel. L. Cargo", "Teleop Lower Cargo")); //Number
//        calculatedColumns.add(new CalculatedColumn("Miss", "Teleop Misses")); //Number
//        calculatedColumns.add(new CalculatedColumn("Pref. Shot Pos.", "Teleop Preferred Shooting Position")); //Selection
//        calculatedColumns.add(new CalculatedColumn("Del. Cargo", "Teleop Delivered Cargo")); //Boolean
//        calculatedColumns.add(new CalculatedColumn("End Climb", "Endgame Climbing Rung")); //Selection
//        calculatedColumns.add(new CalculatedColumn("Def. A", "Defense Defended Against")); //Boolean
//        calculatedColumns.add(new CalculatedColumn("Def. P", "Defense Played Defense")); //Boolean
//        calculatedColumns.add(new CalculatedColumn("Block Shots", "Defense Blocked Shots")); //Number
//        calculatedColumns.add(new CalculatedColumn("Eff. Secs.", "Defense Effective Seconds")); //Number

        return calculatedColumns;
    }

    /**
     * Populate this with all of the various "raw names" either from SumColumns() or the schema.
     * THIS NEEDS TO BE DONE FOR THE CALCULATED COLUMNS - so if you have a calculated column "To. Pieces" then "Total Pieces"
     * needs to be somewhere in here. This is a quirk of using arbitrarily ordered JSON that I never properly fixed.
     *
     * The actual functionality of this list of names is that it determines the order in which these columns appear in raw data
     * @return A list of raw column names which determines the order they appear in the viewer's raw data view
     */
//    public static List<String> PreferredOrder()
//    {
//        return new ArrayList<>(Arrays.asList(
//                "Total Cargo",
//                "Total Upper Cargo", "Total Lower Cargo",
//
//                "Total Upper Cargo",
//                "Auton Upper Cargo", "Teleop Upper Cargo",
//
//                "Total Lower Cargo",
//                "Auton Lower Cargo", "Teleop Lower Cargo",
//
//                "Teleop Misses",
//
//                "Teleop Preferred Shooting Position",
//
//                "Teleop Upper Cargo",
//
//                "Auton Upper Cargo",
//
//                "Endgame Climb Rung",
//
//                "Auton Starting Position",
//
//                "Teleop Lower Cargo",
//
//                "Auton Lower Cargo",
//
//                "Defense Defended Against",
//
//                "Defense Played Defense",
//
//                "Defense Blocked Shots",
//
//                "Defense Effective Seconds",
//
//                "Teleop Delivered Cargo",
//
//                "Auton Cross the Line"
//
//        ));
//
//    }

    /**
     * List of columns to sum, will add a "raw column" for each match scouted with the new calculated value. You might be
     * able to use sum columns in other sum columns, but it is definitely SAFER TO JUST WRITE THEM ALL MANUALLY.
     *
     * Here this is accomplished by using addAll from the other sumcolumns, rather than rewriting everything.
     * @return List of sumcolumns
     */
    public static List<SumColumn> SumColumns() {


        SumColumn upperCargo = BuildSumColumn("Total Upper Cargo", "Teleop Upper Cargo","Auton Upper Cargo");
        SumColumn lowerCargo = BuildSumColumn("Total Lower Cargo", "Teleop Lower Cargo", "Auton Lower Cargo");

        SumColumn totalCargo = new SumColumn();
        totalCargo.columnName = "Total Cargo";
        totalCargo.columnsNames = new ArrayList<>();
        totalCargo.columnsNames.addAll(upperCargo.columnsNames);
        totalCargo.columnsNames.addAll(lowerCargo.columnsNames);


        ArrayList<SumColumn> sumColumns = new ArrayList<>();
        // No auton hatches rn
        addAll(sumColumns, totalCargo, upperCargo, lowerCargo);

        return sumColumns;
    }

    /**
     * Utility macro
     * @param addTo list to populate
     * @param values variable argument array, makes writing this easier
     */
    private static void addAll(List<SumColumn> addTo, SumColumn... values) {
        addTo.addAll(Arrays.asList(values));
    }

    /**
     * Another simple builder
     * @param name "raw name" of the sumcolumn
     * @param columns variable argument array, with the actual other raw names to sum
     * @return SumColumn
     */
    private static SumColumn BuildSumColumn(String name, String... columns) {
        SumColumn column = new SumColumn();
        column.columnName = name;
        column.columnsNames = new ArrayList<>(Arrays.asList(columns));
        return column;
    }

    public static List<String> CalculatedColumnsRawNames() {
        List<String> names = new ArrayList<>();
        for (CalculatedColumn column: CalculatedColumns()){
            names.add(column.RawName);
        }
        return names;
    }

    public static class CalculatedColumn {
        public final String RawName;
        public final String Name;

        public CalculatedColumn(String name, String rawName) {
            RawName = rawName;
            Name = name;
        }
    }

    /**
     * Sum column, can be serialized for easier loading and saving of the table to/from memory
     */
    public static class SumColumn implements Serializable {
        public List<String> columnsNames;
        public String columnName;
    }
}
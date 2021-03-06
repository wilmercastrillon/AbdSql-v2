package clases;

import GeneradorSQL.*;
import TempleateRDB.GenericTempleate.Generic;
import TempleateRDB.TempleateMySQL.GenericMySQL;
import TempleateRDB.TempleatePostgreSQL.GenericPostgreSQL;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import vistas.consola;
import conexionBD.Conexion;
import conexionBD.ConexionPostgreSQL;
import conexionBD.ConexionSQLite;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

public class Fachada {

    private Conexion con;
    private consola cons;
    private String BDseleccionada;
    public GeneradorSQL gen;
    public static String sistemasGestores[] = {"MySQL", "Oracle", "PostgreSQL"};
    private DefaultCompletionProvider provider;

    public Fachada() {
    }

    public Fachada(int tipo) {
        if(tipo == Conexion.PostgreSQL){
            con = new ConexionPostgreSQL();
        }else if(tipo == Conexion.Sqlite){
            con = new ConexionSQLite();
        }else{
            con = new Conexion(tipo);
        }
    }

    public void setTipoConexion(int tipo) {
        if(tipo == Conexion.PostgreSQL){
            con = new ConexionPostgreSQL();
        }else if(tipo == Conexion.Sqlite){
            con = new ConexionSQLite();
        }else{
            con = new Conexion(tipo);
        }
    }

    public boolean conectar(String host, String puerto, String user, String password) {
        BDseleccionada = host;
        cons = new consola(this);
        gen = nuevoGeneradorSQL(con.tipo);
        
        if(con.tipo == Conexion.Sqlite){
            ConexionSQLite lite = (ConexionSQLite) con;
            return lite.conectar(host);
        }else{
            return con.conectar(host, puerto, user, password);
        }
    }

    public void seleccionarBD(String bd) throws SQLException {
        if (con.tipo == Conexion.Oracle || con.tipo == Conexion.Sqlite) {
            return;
        }
        if (con.tipo == Conexion.PostgreSQL) {
            ConexionPostgreSQL aux = new ConexionPostgreSQL();
            aux.conectar(con.getHost(), con.getPuerto(), con.getUsuario(), con.getPasswordText(), bd);
            if (aux.conexionCerrada()) {
                JOptionPane.showMessageDialog(null, "Error en la conexion");
                return;
            }
            con.desconectar();
            con = aux;
        } else {
            ejecutarUpdate(gen.selectDataBase(bd));
        }
        BDseleccionada = bd;
    }

    public String getBDseleccionada() {
        return BDseleccionada;
    }

    public Conexion getConexion() {
        return con;
    }

    public GeneradorSQL getGeneradorSQL() {
        return gen;
    }

    public GeneradorSQL nuevoGeneradorSQL(int tipoSGBD) {
        if (tipoSGBD == Conexion.MySQL) {
            return new GeneradorMySQL();
        } else if (tipoSGBD == Conexion.Oracle) {
            return new GeneradorOracle();
        } else if(tipoSGBD == Conexion.PostgreSQL){
            return new GeneradorPostgreSQL();
        }else{
            return new GeneradorSQLite();
        }
    }

    public String getUsuario() {
        return con.getUsuario();
    }

    public void mostrarConsola() {
        cons.setVisible(true);
    }
    
    public void cerrarConsola() {
        cons.setVisible(false);
    }

    public String inputContraseña(String mensaje, String titulo) {
        String password = "";
        JPasswordField passwordField = new JPasswordField();
        Object[] obj = {mensaje + "\n\n", passwordField};
        Object stringArray[] = {"OK", "Cancel"};
        if (JOptionPane.showOptionDialog(null, obj, titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, stringArray, obj) == JOptionPane.YES_OPTION) {
            password = passwordField.getText();
        }
        return password;
    }

    public boolean recuperarConexion() {
        JOptionPane.showMessageDialog(null, "Conexion perdida!!!", "Error", 0);
        String pass = inputContraseña(con.getHost()+ "\nUsuario: " + con.getUsuario() + "\nIngrese contraseña: ",
                "Recuperar Conexión");

        try {
            if (con.conectar(con.getHost(), con.getPuerto(), con.getUsuario(), pass)) {
                seleccionarBD(BDseleccionada);
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "Error al reconectar", "Error", 0);
            }
        } catch (SQLException ex) {
            return false;
        }
        return false;
    }

    public ResultSet ejecutarConsulta(String sql) throws SQLException {
        if (con.conexionCerrada()) {
            if (!recuperarConexion()) {
                JOptionPane.showMessageDialog(null, "No se pudo recuperar la conexión", "Error", 0);
                return null;
            }
        }
        cons.agregar(sql);
        ResultSet res = con.EjecutarConsulta(sql);
        return res;
    }

    public void ejecutarUpdate(String sql) throws SQLException {
        if (con.conexionCerrada()) {
            if (!recuperarConexion()) {
                JOptionPane.showMessageDialog(null, "No se pudo recuperar la conexión", "Error", 0);
                return;
            }
        }
        cons.agregar(sql);
        con.EjecutarUpdate(sql);
    }

    public Vector<String> getTablesDataBase(String dataBase) throws SQLException {
        Vector<String> aux = new Vector<>();
        ResultSet res2;

        if (con.tipo == Conexion.MySQL) {
            ejecutarUpdate(gen.selectDataBase(dataBase));
            BDseleccionada = dataBase;
        }
        res2 = ejecutarConsulta(gen.getTables());
        while (res2.next()) {
            String h2 = res2.getString(1);
            aux.add(h2);
        }
        return aux;
    }

    public boolean esConexionMysql() {
        return con.tipo == con.MySQL;
    }
    
    public int getTipoConexion(){
        return con.tipo;
    }

    public void actualizarAtributoMysql(String tabla, String nombre, String tipo, String Nuevonombre,
            String longitud, String Default, boolean Nonulo, boolean primera, String despuesDe) throws SQLException {
        GeneradorMySQL cm = (GeneradorMySQL) gen;
        ejecutarUpdate(cm.actualizarAtributo(tabla, nombre, tipo, Nuevonombre, longitud, Default, Nonulo, primera, despuesDe));
    }

    public Vector<String> getBasesDeDatos() throws SQLException {
        Vector<String> aux = new Vector<>();
        if (con.tipo == Conexion.Oracle || con.tipo == Conexion.Sqlite) {
            aux.add(con.getHost());
        } else {
            ResultSet res2 = ejecutarConsulta(gen.getDataBases());
            while (res2.next()) {
                String h2 = res2.getString(1);
                aux.add(h2);
            }
        }
        return aux;
    }

    public Vector<DefaultMutableTreeNode> getTriggers() throws SQLException {
        Vector<DefaultMutableTreeNode> triggers = new Vector<>();
        ResultSet res = ejecutarConsulta(gen.getTriggers());

        while (res.next()) {
            triggers.add(new DefaultMutableTreeNode(res.getString(1)));
        }

        return triggers;
    }

    public Vector<DefaultMutableTreeNode> getProcedimientos(String bd) throws SQLException {
        Vector<DefaultMutableTreeNode> proc = new Vector<>();
        ResultSet res = ejecutarConsulta(gen.getProcedures(bd));
        while (res.next()) {
            proc.add(new DefaultMutableTreeNode(res.getString(1)));
        }

        return proc;
    }

    public String getSqlTrigger(String bd, String nombreTrigger) throws SQLException {
        ResultSet res = ejecutarConsulta(gen.getTriggerData(bd, nombreTrigger));
        res.next();
        return res.getString(3);
    }

    public String getSqlProcedimiento(String bd, String nombreP) throws SQLException {
        String sql = "";
        if (esConexionMysql()) {
            GeneradorMySQL q = (GeneradorMySQL) gen;
            ResultSet p = ejecutarConsulta(q.getParametrosProcedimiento(nombreP));
            String parametros = "";
            while (p.next()) {
                parametros += ", " + p.getString(1);
                parametros += " " + p.getString(2);
            }

            ResultSet res = ejecutarConsulta(gen.getProcedureData(bd, nombreP));
            res.next();
            sql += "CREATE PROCEDURE " + nombreP + "(" + parametros.substring(Math.min(1, parametros.length())) + ")\n";
            sql += res.getString("ROUTINE_DEFINITION");
        } else {
            ResultSet res = ejecutarConsulta(gen.getProcedureData(bd, nombreP));
            res.next();
            sql += "CREATE OR REPLACE ";
            sql += res.getString(1);
        }
        return sql;
    }

    public void llenarTableModel(ResultSet res, DefaultTableModel modeloJtable) throws SQLException {
        ResultSetMetaData rsmd = res.getMetaData();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            modeloJtable.addColumn(rsmd.getColumnName(i));
        }

        while (res.next()) {
            Vector<String> datos = new Vector<>();
            for (int i = 1; i <= modeloJtable.getColumnCount(); i++) {
                datos.add(res.getString(i));
            }
            modeloJtable.addRow(datos);
        }
    }

    public Vector<String> getDatosColumna(JTable modelo, String Columna) {
        Vector<String> datos = new Vector<>();//nombre, tipo, default, nulo y longitud.
        int index = -1;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.getValueAt(i, 0).toString().equals(Columna)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return null;
        }

        datos.add(modelo.getValueAt(index, 0).toString());//nombre
        String tipo = modelo.getValueAt(index, 1).toString().toUpperCase();//tipo
        int parentesis = tipo.indexOf("(");
        if (parentesis == -1) {
            datos.add(tipo);
        } else {
            datos.add(tipo.substring(0, parentesis));
        }

        Object obj = modelo.getValueAt(index, 3);//default
        if (obj == null) {
            datos.add(null);
        } else {
            datos.add(obj.toString());
        }

        datos.add(modelo.getValueAt(index, 2).toString());//nulo

        if (parentesis == -1) {//longitud
            datos.add("");
        } else {
            datos.add(tipo.substring(parentesis + 1, tipo.length() - 1));
        }

        return datos;
    }

    public Vector<Vector<String>> cargarTipos() {
        Vector<Vector<String>> vvs = new Vector<>();

        String nombre = "";
        if (con.tipo == Conexion.MySQL) {
            nombre = "tiposMySQL";
        } else if (con.tipo == Conexion.PostgreSQL) {
            nombre = "tiposPostgreSQL";
        }
        try {
            System.out.println(System.getProperty("user.dir") + "\\data\\" + nombre);
            BufferedReader tec = new BufferedReader(new FileReader(System.getProperty("user.dir") + "\\data\\" + nombre));
            while (tec.ready()) {
                String s = tec.readLine();
                String in[] = s.split("/");

                Vector<String> vs = new Vector<>();
                for (String in1 : in) {
                    vs.add(in1);
                }
                System.out.println(Arrays.toString(vs.toArray()));
                vvs.add(vs);
            }
        } catch (Exception e) {
            return null;
        }
        return vvs;
    }

    public ArrayList<String> cargarDatosConexion() {
        ArrayList<String> vs = new ArrayList<>();
        try {
            System.out.println(System.getProperty("user.dir") + "\\config");
            BufferedReader tec = new BufferedReader(new FileReader(System.getProperty("user.dir") + "\\config"));
            while (tec.ready()) {
                String s = tec.readLine();
                s += " " + tec.readLine();
                s += " " + tec.readLine();
                s += " " + tec.readLine();
                vs.add(s);
            }
        } catch (Exception e) {
            if (vs.isEmpty()) {
                return null;
            }
        }
        return vs;
    }

    public void guardarDatosConexion(ArrayList<String> vs) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "\\config"));
            String s[];
            for (int i = 0; i < vs.size(); i++) {
                s = vs.get(i).split(" ");
                for(String cad : s){
                    bw.write(cad + "\r\n");
                }
            }
            bw.close();
        } catch (Exception e) {
        }
    }

    public boolean generaraMVC(Vector<String> tablas, String ruta) {
        GeneradorMVC gen = new GeneradorMVC(tablas, this);
        try {
            gen.GenerarModelos(ruta);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean exportar(String ruta, int tipo) {
        ExportadorSQL exp = new ExportadorSQL(this, tipo);
        try {
            exp.exportar(BDseleccionada, ruta, "script");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean generaraDiccionario(String ruta, String BD) {
        GeneradorDiccionario dd = new GeneradorDiccionario(this, BD);
        try {
            dd.crearWord(ruta, "diccionario.docx");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            return false;
        }
        return true;
    }

    public boolean convertirPostgreSQL(String ruta) {
        return exportar(ruta, Conexion.PostgreSQL);
    }

    public void exportarSQL(String ruta, int sistema) throws SQLException, IOException {
        if (ruta.endsWith("\\")) {
            ruta += "script.sql";
        }else{
            ruta += "\\script" + ".sql";
        }
        
        Generic prueba;
        if (sistema == Conexion.MySQL) {
            prueba = new GenericMySQL(BDseleccionada);
        }else{
            prueba = new GenericPostgreSQL(BDseleccionada);
        }
        prueba.loadDatabase(con.Getconeccion());
        BufferedWriter tec = new BufferedWriter(new FileWriter(ruta));
        tec.write("");
        tec.write(prueba.DatabaseToSqlWithData(con.Getconeccion()));
        tec.close();
    }

    private CompletionProvider cargarProvider() throws IOException {
        BufferedReader tec = new BufferedReader(new FileReader(System.getProperty("user.dir")
                + "\\data\\autocompleteWords"));
        provider = new DefaultCompletionProvider();
        while (tec.ready()) {
            provider.addCompletion(new BasicCompletion(provider, tec.readLine()));
        }
        return provider;
    }

    public CompletionProvider getProvider() throws IOException {
        if (provider == null) {
            cargarProvider();
        }
        return provider;
    }
    
    public ArrayList<String> getMysqlDataTypes(){
        String str = "TINYINT,SMALLINT,INT,MEDIUMINT,BIGINT,FLOAT,DOUBLE,CHAR,VARCHAR,TINYTEXT,TEXT,";
        str += "LONGTEXT,DATE,TIME,YEAR,DATETIME,TIMESTAMP,BYNARY,VARBINARY,BLOB,MEDIUMBLOB,LONGBLOB,ENUM";
        
        ArrayList<String> vs = new ArrayList<>();
        String x[] = str.split(",");
        vs.addAll(Arrays.asList(x));
        return vs;
    }
    
    public ArrayList<String> getPostgresDataTypes(){
        String str = "SMALLINT,INTEGER,BIGINT,FLOAT,DOUBLE,FLOAT8,DECIMAL,BOOLEAN,CHAR,VARCHAR,";
        str += "CHARACTER VARYING,TEXT,DATE,TIME,TIMESTAMP,BYTEA,BIT,VARBIT,CIDR,INET";
        
        ArrayList<String> vs = new ArrayList<>();
        String x[] = str.split(",");
        vs.addAll(Arrays.asList(x));
        return vs;
    }
    
    public ArrayList<String> getOracleDataTypes(){
        String str = "NUMBER,FLOAT,BINARY_DOUBLE,CHAR,VARCHAR2,NCHAR,NVARCHAR2,LONG,DATE,TIMESTAMP,";
        str += "CLOB,NCLOB,BFILE,RAW,LONG RAW";
        
        ArrayList<String> vs = new ArrayList<>();
        String x[] = str.split(",");
        vs.addAll(Arrays.asList(x));
        return vs;
    }
    
    public ArrayList<String> getSQLiteDataTypes(){
        String str = "INTEGER,REAL,NUMERIC,TEXT,BLOB";
        
        ArrayList<String> vs = new ArrayList<>();
        String x[] = str.split(",");
        vs.addAll(Arrays.asList(x));
        return vs;
    }
}

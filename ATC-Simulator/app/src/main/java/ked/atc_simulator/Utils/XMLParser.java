package ked.atc_simulator.Utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Xml;

import ked.atc_simulator.Entities.Plane;

/* Credits : Iza Marfisi  */

public class XMLParser {


    /**
     * Cette classe représente une "entry" dans le fichier XML
     * elle contient les informations sur les attributs "nom" et "message"
     */
    public class Entry {

        // Attributs de la classe Entry
        public final String nom;
        public final float x, y, heading;
        public final String route, planeState;


        // Constructeur de la classe Entry
        private Entry(String nom, float x, float y, float heading, String route, String planeState) {
            this.nom = nom;
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.route = route;
            this.planeState = planeState;
        }
    }


    /**
     * Cette fonction parse le contenu d'un fichier et retourne le résultat
     *
     * @param in le fichier contenant le XML
     * @return une liste d'objet de type Entry
     */
    public List<Entry> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readData(parser);
        } finally {
            in.close();
        }
    }


    /**
     * Cette fonction parse le contenu d'une balise <entry> et retourne le résultat
     *
     * @param parser le bout de XML
     * @return une liste d'objet de type Entry
     */
    private List<Entry> readData(XmlPullParser parser) throws XmlPullParserException, IOException {
        // création d'une nouvelle liste d'objets de type Entry
        // que l'obn va remplir avec le contenu du XML
        List<Entry> entries = new ArrayList<>();

        // le XML doit commencer par "<data>"
        parser.require(XmlPullParser.START_TAG, null, "data");

        // tand que l'élément suivant n'est pas une balise fermante </..>
        while (parser.next() != XmlPullParser.END_TAG) {
            // si le XML est une de balise ouvrante <..>, continuer
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            // si cette balise est un <entry>, extraire le contenu de cette balise avec readEntry()
            // et ajouter le nouvel object Entry dans la liste entries
            if (name.equals("entry")) {
                entries.add(readEntry(parser));
                // sinon, sauter la balise
            } else {
                skip(parser);
            }
        }
        // retourner toute la liste des Entry, qui viennent d'être remplis
        return entries;
    }


    /**
     * Cette fonction parse le contenu d'une balise <entry>
     * Si elle rencontre une balise de type <nom> ou <message>, elle fait appel à readTag() pour extraire le contenu de la balise
     * Sinon, elle saute la balise avec skip()
     *
     * @param parser le bout de XML
     * @return un object de type Entry qui contient les données extrait du XML
     */
    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        // le XML doit commencer par "<entry>"
        parser.require(XmlPullParser.START_TAG, null, "entry");
        String nom = null;
        float x = -1f;
        float y = -1f;
        float heading = -1f;
        String route = null;
        String planeState = null;

        // tand que l'élément suivant n'est pas une balise fermante </..>
        while (parser.next() != XmlPullParser.END_TAG) {
            // si le XML est une de balise ouvrante <..>, continuer
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            // si cette balise est un <nom>, extraire le nom
            switch (name) {
                case "nom":
                    nom = readTag(parser, "nom");

                    //si cette balise est un <message>, extraire le message
                    break;
                case "x":
                    x = Float.parseFloat(readTag(parser, "x"));

                    // sinon, sauter la balise
                    break;
                case "y":
                    y = Float.parseFloat(readTag(parser, "y"));

                    // sinon, sauter la balise
                    break;
                case "heading":
                    heading = Float.parseFloat(readTag(parser, "heading"));

                    // sinon, sauter la balise
                    break;
                case "route":
                    route = readTag(parser, "x");

                    // sinon, sauter la balise
                    break;
                case "planeState":
                    planeState = readTag(parser, "planeState");

                    // sinon, sauter la balise
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        // retourner une nouvelle Entry avec le nom et message extrait du XML
        return new Entry(nom, x, y, heading, route, planeState);
    }


    /**
     * Cette fonction extrait le contenu d'une balise avec le titre tag
     *
     * @param parser le bout de XML
     * @param tag    le titre de la balise (ex: "nom", "message")
     * @return le contenu String de la balise
     */
    private String readTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        String result = "";
        // le XML doit commencer par "<tag>"
        parser.require(XmlPullParser.START_TAG, null, tag);
        /* si l'élément suivant est un text, sauvegarder le text dans result
        et passer à l'élément suivant */
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        // le XML doit finir par "</tag>"
        parser.require(XmlPullParser.END_TAG, null, tag);
        return result;
    }


    /**
     * Cette fonction saute une balise entière, y compris les autres balises qui sont à l'interieur
     *
     * @param parser le bout de XML
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public void write(Context context, ArrayList<Plane> planes) throws FileNotFoundException, IOException {

        FileOutputStream fos = new FileOutputStream(context.getFilesDir() + "/ATC-Simulator.xml");
        FileOutputStream fileos = context.openFileOutput("ATC-Simulator", Context.MODE_PRIVATE);
        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        xmlSerializer.setOutput(writer);
        xmlSerializer.startDocument("UTF-8", true);
        for(Plane p : planes) {
            xmlSerializer.startTag(null, "entry");
            xmlSerializer.startTag(null, "nom");
            xmlSerializer.text(p.getName());
            xmlSerializer.endTag(null, "nom");
            xmlSerializer.startTag(null, "x");
            xmlSerializer.text("" + p.getBase().x);
            xmlSerializer.endTag(null, "x");
            xmlSerializer.startTag(null, "y");
            xmlSerializer.text("" + p.getBase().y);
            xmlSerializer.endTag(null, "y");
            xmlSerializer.startTag(null, "heading");
            xmlSerializer.text("" + p.getHeading());
            xmlSerializer.endTag(null, "heading");
            xmlSerializer.startTag(null, "route");
            xmlSerializer.text(p.getRoute().getName());
            xmlSerializer.endTag(null, "route");
            xmlSerializer.startTag(null, "planeState");
            xmlSerializer.text(p.getPlaneState().getName());
            xmlSerializer.endTag(null, "planeState");
            xmlSerializer.endTag(null, "entry");
        }
        xmlSerializer.endDocument();
        xmlSerializer.flush();
        String dataWrite = writer.toString();
        fileos.write(dataWrite.getBytes());
        fileos.close();
    }

}


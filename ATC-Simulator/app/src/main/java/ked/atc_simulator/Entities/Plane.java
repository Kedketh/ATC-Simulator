package ked.atc_simulator.Entities;


import android.util.Log;

import ked.atc_simulator.Canvas.PlanePath;
import ked.atc_simulator.Canvas.Point;
import ked.atc_simulator.GameActivity;
import ked.atc_simulator.Gameplay.Route;
import ked.atc_simulator.Gameplay.RunwayRoute;
import ked.atc_simulator.State.ArrivingState;
import ked.atc_simulator.State.PlaneState;
import ked.atc_simulator.Utils.CoordinateConverter;

/**
 * Cette classe gère tous les attributs des avions
 * Notamment le calcul de la position
 */
public class Plane {

    private PlanePath path;
    private float heading, speed;
    private Point base;
    private Route route;
    private GameActivity context;
    private PlaneState planeState;
    private int behavior; // 0 "normal",  1 "holding" -> Stays in the holding circuit, 2 "runway" -> waits before runway entrance, 3 "waiting" -> stops until further notice
    private String name;
    private boolean markedForRemoval;

    /**
     * Constructeur de la classe Plane
     * @param context
     * @param name - Nom de l'avion
     * @param x
     * @param y
     * @param heading - Cap
     * @param route - La route actuelle de l'avion
     * @param planeState - L'état de l'avion (object ArrivingState ou DepartingState)
     */
    public Plane(GameActivity context,String name, float x, float y, float heading, Route route, PlaneState planeState) {
        base = new Point(x, y);
        this.route = route;
        speed = route.getSpeed();
        this.heading = heading;
        path = new PlanePath(context, base, heading);
        this.context = context;
        this.planeState = planeState;
        behavior = 1;
        this.name = name;
        markedForRemoval = false;
    }

    /**
     * Constructeur de la classe Plane
     * @param context
     * @param name - Nom de l'avion
     * @param x
     * @param y
     * @param heading - Cap
     * @param behavior - Comportement de l'avion 0 = normal; 1 = holding (l'avion tourne autour de l'aéroport en attendant l'autorisation à l'attérissage; 2 = runway (l'avion s'arrête avant la piste jusqu'à l'autorisation de décoller); 3 = Stop (l'avion s'arrête si il est au sol)
     * @param route - La route actuelle de l'avion
     * @param planeState - L'état de l'avion (object ArrivingState ou DepartingState)
     */
    public Plane(GameActivity context,String name, float x, float y, float heading,int behavior, Route route, PlaneState planeState) {
        base = new Point(x, y);
        this.route = route;
        speed = route.getSpeed();
        this.heading = heading;
        path = new PlanePath(context, base, heading);
        this.context = context;
        this.planeState = planeState;
        this.behavior = behavior;
        this.name = name;
        markedForRemoval = false;
    }

    /**
     * Constructeur vide pour créer un objet EmptyPlane permettant de vérifier quand Plane est vide
     */
    public Plane(){}

    /** Getter pour le path de l'avion
     * @return
     */
    public PlanePath getPath() {
        return path;
    }

    /**
     * Setter pour la route de l'avion
     * @param route
     */
    public void setRoute(Route route) {
        this.route = route;
        this.heading = route.getHeading();
    }

    /**
     * Setter pour le comportement de l'avion
     * @param b
     */
    public void setBehavior(int b) {
        if (b == 3 && (!route.getName().equals("Alpha") && !route.getName().equals("Bravo") && !route.getName().equals("Charlie"))) {
            behavior = 2;
        } else behavior = b;
        Log.i("Refresh", "Setting behavior to " + b);
    }

    /**
     *  Getter pour la base (la position de l'avion)
     * @return
     */
    public Point getBase(){ return base; }

    /**
     * Setter pour la base (la position de l'avion)
     * @return
     */
    public Route getRoute(){ return route; }

    /**
     * Getter pour le comportement de l'avion
     * @return
     */
    public int getBehavior(){return behavior; }

    /**
     * Getter pour le cap de l'avion
     * @return
     */
    public float getHeading() {
        return heading;
    }

    /**
     * Getter pour l'état de l'avion
     * @return
     */
    public PlaneState getPlaneState() {
        return planeState;
    }

    /**
     * Setter pour l'état de l'avion
     * @param planeState
     */
    public void setPlaneState(PlaneState planeState) {
        this.planeState = planeState;
    }

    /**
     * Getter pour le nom de l'avion
     * @return
     */
    public String getName() { return name; }

    /**
     * Getter pour le marquage de l'avion
     * Le marquage permet de désigner l'avion comme "à supprimer" au prochain rafraichissement
     * @return
     */
    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    /**
     * Calcule les nouvelles positions de l'avion en fonction de sa route, sa vitesse, son cap, ...
     */
    public void calculateNewParams() {
        if (behavior != 3) { // Si l'avion ne doit pas s'arrêter
            if (route.getNextRoute() != null) {
                //Ces variables correspondent à la différence entre les coordonées de l'avion et celles de l'entrée de la prochaine route
                float diffX = CoordinateConverter.GetXDipsFromCoordinate(context, base.x - route.getNextRoute().getStartPoint().x);
                float diffY = CoordinateConverter.GetXDipsFromCoordinate(context, base.y - route.getNextRoute().getStartPoint().y);

                Log.i("Refresh", "Calculating new params routeName: " + route.getName() + ", speed : " + (speed / 3) + ", diffX: " + diffX + " , diffY: " + diffY);
                /*Le coefficient de précision d'une route permet d'être plus ou moins précis dans le passage à la route suivante en fonction
                    de la vitesse où est l'avion sur la route */
                int pcx = route.getNextRoute().getPrecisionCoefX();
                int pcy = route.getNextRoute().getPrecisionCoefY();

                //passage à la route suivante
                if (diffX <= (speed / pcx) && diffX > -(speed / pcx) && diffY <= (speed / pcy) && diffY > -(speed / pcy)) {
                    if (route.getNextRoute() instanceof RunwayRoute && behavior == 2) { // Si l'avion doit attendre avant la piste, on l'arrête
                        behavior = 3;
                        return; // et on ne va pas plus loin
                    } else route = route.getNextRoute();
                    Log.i("Refresh", "Switching route " + route.getName());
                }
                //Différentes actions en fonction de l'état de l'avion (arrivée ou départ). Utilisation du StatePattern
                if (route.getName().equals("Base") && behavior != 1 && !planeState.baseAction().equals(null) && !route.getNextRoute().equals(planeState.baseAction())) {
                    route.setNextRoute(planeState.baseAction());
                    Log.i("RefreshState", "BaseAction , routeName : " + route.getNextRoute().getName());
                } else if (route.getName().equals("CrosswindRN") && behavior != 1 && !planeState.crosswindRNAction().equals(null) && !route.getNextRoute().equals(planeState.crosswindRNAction())) {
                    route.setNextRoute(planeState.crosswindRNAction());
                    Log.i("RefreshState", "CrosswindRNAction , routeName : " + route.getName());
                } else if (route.getName().equals("Final") && (diffY >= 5 || diffY <= 2)) {
                    base.y = route.getStartPoint().y + 10;
                    Log.i("RefreshState", "Final Action");
                }else if(route.getName().equals("Charlie") && planeState instanceof ArrivingState){
                    behavior = 3;//Stop
                    markedForRemoval = true;
                }
                //On met a jour le cap et la vitesse en fonction de la route empruntée
                heading = route.getHeading();
                speed = route.getSpeed();

                //La vitesse varie quand on est sur une piste (que ce soit au décollage ou à l'atterissage)
                // en fonction de la position de l'avion sur ce dernier
                if (route instanceof RunwayRoute) {
                    float endx = (route.getStartPoint().x + ((RunwayRoute) route).getLenght());
                    if (route.getName().equals("RunwayTO"))
                        speed = route.getSpeed() * (base.x / endx);
                    else speed = route.getSpeed() * (route.getStartPoint().x / base.x);
                    Log.i("Speed", "runway speed:" + (route.getSpeed() / (base.x / endx)));
                }
            }
            //Mise à jour de la position de l'avion
            base.x += ((speed / 2) / 3.6) * Math.cos(Math.toRadians(heading - 90));
            base.y += ((speed / 2) / 3.6) * Math.sin(Math.toRadians(heading - 90));
            Log.i("Refresh", "Calculating new params : x = " + base.x + ", y = " + base.y);
        }
        //On calcule la position des points de l'avion avant de le dessiner
        path.updatePoints(base, heading);
    }

    /* Cette fonction vérifie que l'avion est toujours sur l'écran de façon à supprimer ceux qui en sortent
     *
     */
    public boolean isOutOfScreen(){
        if(base.x > 1920 || base.x < 0 || base.y < 0 || base.y > 1080) {//Coordonées, pas DiP
            Log.i("Cleanup",name+" is currently out of the screen");
            return true;
        }else return false;
    }
}

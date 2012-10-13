package com.bfv.view;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 1/10/12
 * Time: 9:18 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ParamatizedComponent {

    public static int TYPE_VIEW_COMPONENT = 0;
    public static int TYPE_VIEW_PAGE = 1;
    public static int TYPE_MAP_OVERLAY = 2;

    public int getParamatizedComponentType();

    public String getParamatizedComponentName();

    public ArrayList<ViewComponentParameter> getParameters();

    public void setParameterValue(ViewComponentParameter parameter);
}

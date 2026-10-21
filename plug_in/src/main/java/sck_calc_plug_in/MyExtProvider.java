package sck_calc_plug_in;

import info.openrocket.core.plugin.Plugin;
import info.openrocket.core.simulation.extension.AbstractSimulationExtensionProvider;

@Plugin
public class MyExtProvider extends AbstractSimulationExtensionProvider {
    public MyExtProvider() {
        super(MyExt.class, "Shock Load", "Shock Load Calculator");
    }
}
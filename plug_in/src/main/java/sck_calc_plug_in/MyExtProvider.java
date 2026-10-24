package sck_calc_plug_in;

import info.openrocket.core.plugin.Plugin;
import info.openrocket.core.simulation.extension.AbstractSimulationExtensionProvider;

@Plugin
public class MyExtProvider extends AbstractSimulationExtensionProvider {
    public MyExtProvider() {
        // First string: top-level menu. Second string: menu item.
        super(MyExt.class, "Shock Loads", "Shock Load Calculator");
    }
}

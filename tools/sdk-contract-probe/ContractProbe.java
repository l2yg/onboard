import ai.pleos.playground.location.fused.model.ProximityAlertRequest;
import ai.pleos.playground.location.fused.model.ProximityTransition;
import ai.pleos.playground.navi.data.NavigationScreenRequest;
import ai.pleos.playground.vehicle.api.model.capability.AccessType;
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig;
import ai.pleos.playground.vehicle.api.model.common.TripStatistics;
import ai.pleos.playground.vehicle.constant.control.TripStatisticType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/** Runs only JVM model contracts from official SDK binaries; never connects to a service. */
public final class ContractProbe {
    private static int passed;

    private static void check(String name, boolean result) {
        if (!result) throw new AssertionError(name);
        passed++;
        System.out.println("PASS " + name);
    }

    private static void proximity(String name, double lat, double lon, float radius,
                                  Set<ProximityTransition> transitions, boolean valid)
            throws Exception {
        ProximityAlertRequest request = new ProximityAlertRequest(lat, lon, radius, transitions);
        Method validator = List.of(ProximityAlertRequest.class.getDeclaredMethods()).stream()
                .filter(m -> m.getName().startsWith("validationErrorOrNull"))
                .findFirst().orElseThrow();
        Object error = validator.invoke(request);
        check(name, valid ? error == null : error instanceof String && !((String) error).isEmpty());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void area(String kind, String valueKind) throws Exception {
        Class<?> areaClass = Class.forName("ai.pleos.playground.vehicle.api.model.area." + kind);
        Class<?> valueClass = Class.forName("ai.pleos.playground.vehicle.constant.area." + valueKind);
        Object[] constants = valueClass.getEnumConstants();
        Object selected = Array.newInstance(valueClass, 2);
        Array.set(selected, 0, constants[0]);
        Array.set(selected, 1, constants[1]);
        Object first = areaClass.getConstructor(selected.getClass()).newInstance(selected);
        Object copy = Array.newInstance(valueClass, 2);
        Array.set(copy, 0, constants[0]);
        Array.set(copy, 1, constants[1]);
        Object second = areaClass.getConstructor(copy.getClass()).newInstance(copy);
        check(kind + " same ordered values compare equal", first.equals(second));
        check(kind + " equal values have same hash", first.hashCode() == second.hashCode());
        Array.set(copy, 0, constants[1]);
        Array.set(copy, 1, constants[0]);
        Object reversed = areaClass.getConstructor(copy.getClass()).newInstance(copy);
        check(kind + " reversed values are different map keys", !first.equals(reversed));
    }

    public static void main(String[] args) throws Exception {
        Set<ProximityTransition> enter = Set.of(ProximityTransition.ENTER);
        proximity("latitude/longitude lower boundary", -90, -180, 1, enter, true);
        proximity("latitude/longitude upper boundary", 90, 180, 1, enter, true);
        proximity("both transitions", 0, 0, Float.MIN_VALUE,
                Set.of(ProximityTransition.ENTER, ProximityTransition.EXIT), true);
        proximity("latitude below boundary", -90.001, 0, 1, enter, false);
        proximity("latitude above boundary", 90.001, 0, 1, enter, false);
        proximity("latitude NaN", Double.NaN, 0, 1, enter, false);
        proximity("latitude infinite", Double.POSITIVE_INFINITY, 0, 1, enter, false);
        proximity("longitude below boundary", 0, -180.001, 1, enter, false);
        proximity("longitude above boundary", 0, 180.001, 1, enter, false);
        proximity("longitude NaN", 0, Double.NaN, 1, enter, false);
        proximity("longitude infinite", 0, Double.NEGATIVE_INFINITY, 1, enter, false);
        proximity("radius zero", 0, 0, 0, enter, false);
        proximity("radius negative", 0, 0, -1, enter, false);
        proximity("radius NaN", 0, 0, Float.NaN, enter, false);
        proximity("radius infinite", 0, 0, Float.POSITIVE_INFINITY, enter, false);
        proximity("empty transitions", 0, 0, 1, Set.of(), false);

        check("default capability is NONE", new CapabilityConfig().getAccessType() == AccessType.NONE);
        TripStatistics<Integer> defaults = new TripStatistics<>();
        check("default trip raw is empty", defaults.getRaw().isEmpty());
        for (TripStatisticType kind : TripStatisticType.values()) {
            check("default trip " + kind + " is null", defaults.get(kind) == null);
        }
        TripStatistics<Integer> values = new TripStatistics<>(11, 22, 33, 44, List.of(99));
        check("CURRENT selects current field", values.get(TripStatisticType.CURRENT) == 11);
        check("INSTANTANEOUS selects instantaneous field", values.get(TripStatisticType.INSTANTANEOUS) == 22);
        check("CUMULATIVE selects cumulative field", values.get(TripStatisticType.CUMULATIVE) == 33);
        check("SINCE_REFILL selects sinceRefill field", values.get(TripStatisticType.SINCE_REFILL) == 44);

        area("DoorArea", "DoorValue");
        area("MirrorArea", "MirrorValue");
        area("SeatArea", "SeatValue");
        area("TurnSignalArea", "TurnSignalValue");
        area("WheelArea", "WheelValue");
        area("WindowArea", "WindowValue");

        NavigationScreenRequest.POIDetailPopup popup = new NavigationScreenRequest.POIDetailPopup("poi", "provider");
        check("POI detail keeps provider", "provider".equals(popup.getPoiProvider()));
        check("POI detail keeps ID", "poi".equals(popup.getPoiId()));
        System.out.println("TOTAL " + passed + " passed; no service calls.");
    }
}

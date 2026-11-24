package io.github.leejihoon1592.blacksmith.research;

import java.util.*;
import io.github.leejihoon1592.blacksmith.material.MetalIngotItem.Stats;

/** Runs without a world or client; failures abort Gradle check. */
public final class ThermalResearchTest {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static ThermalExperiment.Component part(String id,int temp,int ticks,String amount) { return new ThermalExperiment.Component(id,temp,ticks,1,amount); }
    public static void main(String[] args) {
        var components=List.of(part("host",100,3,"very_many"),part("byproduct",200,2,"little"),part("metal",300,4,"many"));
        var experiment=new ThermalExperiment(components);
        var notes=new ArrayList<ThermalExperiment.Observation>();
        for(int i=0;i<3;i++) notes.addAll(experiment.tick(100));
        for(int i=0;i<2;i++) notes.addAll(experiment.tick(200));
        for(int i=0;i<4;i++) notes.addAll(experiment.tick(300));
        check(notes.size()==6,"Three separable components must produce six observations");
        check(notes.stream().filter(ThermalExperiment.Observation::onset).count()==3,"Three onset events");
        check(notes.stream().noneMatch(ThermalExperiment.Observation::mixed),"Separated batches must remain identifiable");
        check(notes.get(1).quantity().equals("very_many") && notes.get(3).quantity().equals("little") && notes.get(5).quantity().equals("many"),"Quantity must describe component, not order");
        check(experiment.complete() && experiment.tick(1000).isEmpty(),"No duplicate observations after completion");
        var mixed=new ThermalExperiment(components);
        var mixedNotes=new ArrayList<ThermalExperiment.Observation>();
        for(int i=0;i<4;i++) mixedNotes.addAll(mixed.tick(400));
        check(mixedNotes.stream().filter(n->!n.onset()).allMatch(ThermalExperiment.Observation::mixed),"Overlapping melts must be ambiguous");
        var pause=new ThermalExperiment(List.of(part("metal",100,3,"many")));
        pause.tick(100);pause.tick(20);pause.tick(20);
        check(!pause.complete() && pause.progress().get(0).activeTicks==1,"Below threshold must pause melting");
        pause.tick(100);var end=pause.tick(100);
        check(end.get(0).elapsedTicks()==5,"Observed elapsed duration includes cooling pauses");
        var restored=new ThermalExperiment(components);
        var original=new ThermalExperiment(components);original.tick(250);
        for(int i=0;i<3;i++) {
            var a=original.progress().get(i);var b=restored.progress().get(i);
            b.activeTicks=a.activeTicks;b.elapsedTicks=a.elapsedTicks;b.started=a.started;b.done=a.done;b.mixed=a.mixed;
        }
        check(original.tick(250).equals(restored.tick(250)),"Restored progress must yield identical events");
        Stats dur=new Stats(8,7,5,3.5,1.5);
        check(MaterialPhysics.meltingPoint(dur)==1660,"Sheet3 melting point");
        check(MaterialPhysics.meltingSeconds(dur,30)==51,"Specialization baseline included");
        check(MaterialPhysics.meltingSeconds(dur,15)==36,"Baseline must not be hardcoded");
        check(MaterialPhysics.weight(dur)==13,"Preserve decimal gaps in weight correction");
        check(MaterialPhysics.forgingMinimum(dur)==1170 && MaterialPhysics.forgingMaximum(dur)==1500,"Forge temperatures round upward to tens");
        System.out.println("Thermal research checks passed: separated/mixed/cooling/resume/formulas.");
    }
}

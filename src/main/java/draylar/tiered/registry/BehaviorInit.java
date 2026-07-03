package draylar.tiered.registry;

import draylar.tiered.api.imprint.behavior.AfflictionBehavior;
import draylar.tiered.api.imprint.behavior.AirborneMeleeBehavior;
import draylar.tiered.api.imprint.behavior.BloodthirstBehavior;
import draylar.tiered.api.imprint.behavior.CondemnedBehavior;
import draylar.tiered.api.imprint.behavior.ExecutionerBehavior;
import draylar.tiered.api.imprint.behavior.FlankingBehavior;
import draylar.tiered.api.imprint.behavior.ImprintBehaviorRegistry;
import draylar.tiered.api.imprint.condition.ScaleConditionRegistry;
import draylar.tiered.api.imprint.behavior.LastStandBehavior;
import draylar.tiered.api.imprint.behavior.UniversalDamageBehavior;
import draylar.tiered.api.imprint.behavior.MomentumBehavior;
import draylar.tiered.api.imprint.behavior.RavenousBehavior;
import draylar.tiered.api.imprint.behavior.ReinforceBehavior;
import draylar.tiered.api.imprint.behavior.RetaliationBehavior;
import draylar.tiered.api.imprint.behavior.SecondWindBehavior;
import draylar.tiered.api.imprint.behavior.SoulHarvestBehavior;
import draylar.tiered.api.imprint.behavior.SprintMeleeBehavior;
import draylar.tiered.api.imprint.behavior.StalwartBehavior;
import draylar.tiered.api.imprint.behavior.TemperedBehavior;
import draylar.tiered.api.imprint.behavior.ThornsAuraBehavior;
import draylar.tiered.api.imprint.behavior.TormentedBehavior;
import draylar.tiered.api.imprint.behavior.WitherStrikeBehavior;

public final class BehaviorInit {

    private BehaviorInit() {}

    public static void init() {
        ScaleConditionRegistry.init();
        ImprintBehaviorRegistry.register(SprintMeleeBehavior.ID, new SprintMeleeBehavior());
        ImprintBehaviorRegistry.register(AirborneMeleeBehavior.ID, new AirborneMeleeBehavior());
        ImprintBehaviorRegistry.register(UniversalDamageBehavior.ID, new UniversalDamageBehavior());
        ImprintBehaviorRegistry.register(ReinforceBehavior.ID, new ReinforceBehavior());
        ImprintBehaviorRegistry.register(SecondWindBehavior.ID, new SecondWindBehavior());
        ImprintBehaviorRegistry.register(RetaliationBehavior.ID, new RetaliationBehavior());
        ImprintBehaviorRegistry.register(ThornsAuraBehavior.ID, new ThornsAuraBehavior());
        ImprintBehaviorRegistry.register(BloodthirstBehavior.ID, new BloodthirstBehavior());
        ImprintBehaviorRegistry.register(WitherStrikeBehavior.ID, new WitherStrikeBehavior());
        ImprintBehaviorRegistry.register(SoulHarvestBehavior.ID, new SoulHarvestBehavior());
        ImprintBehaviorRegistry.register(ExecutionerBehavior.ID, new ExecutionerBehavior());
        ImprintBehaviorRegistry.register(LastStandBehavior.ID, new LastStandBehavior());
        ImprintBehaviorRegistry.register(MomentumBehavior.ID, new MomentumBehavior());
        ImprintBehaviorRegistry.register(FlankingBehavior.ID, new FlankingBehavior());
        ImprintBehaviorRegistry.register(CondemnedBehavior.ID, new CondemnedBehavior());
        ImprintBehaviorRegistry.register(TormentedBehavior.ID, new TormentedBehavior());
        ImprintBehaviorRegistry.register(AfflictionBehavior.ID, new AfflictionBehavior());
        ImprintBehaviorRegistry.register(RavenousBehavior.ID, new RavenousBehavior());
        ImprintBehaviorRegistry.register(TemperedBehavior.ID, new TemperedBehavior());
        ImprintBehaviorRegistry.register(StalwartBehavior.ID, new StalwartBehavior());
    }
}

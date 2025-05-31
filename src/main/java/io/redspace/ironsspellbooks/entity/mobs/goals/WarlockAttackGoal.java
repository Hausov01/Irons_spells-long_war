package io.redspace.ironsspellbooks.entity.mobs.goals;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class WarlockAttackGoal extends WizardAttackGoal {

    protected float meleeRange;
    protected boolean wantsToMelee;
    protected int meleeTime;
    protected int meleeDecisionTime;
    protected float meleeBiasMin;
    protected float meleeBiasMax;
    protected float meleeMoveSpeedModifier;
    protected int meleeAttackIntervalMin;
    protected int meleeAttackIntervalMax;
    public WarlockAttackGoal(IMagicEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval, float meleeRange) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
        this.meleeRange = meleeRange;
        this.meleeDecisionTime = mob.getRandom().nextIntBetweenInclusive(80, 200);
        this.meleeBiasMin = .25f;
        this.meleeBiasMax = .75f;
        this.allowFleeing = false;
        this.meleeMoveSpeedModifier = (float) pSpeedModifier;
        this.meleeAttackIntervalMin = minAttackInterval;
        this.meleeAttackIntervalMax = maxAttackInterval;
    }

    @Override
    public void tick() {
        super.tick();
        if (++meleeTime > meleeDecisionTime) {
            meleeTime = 0;
            wantsToMelee = mob.getRandom().nextFloat() <= meleeBias();
            meleeDecisionTime = mob.getRandom().nextIntBetweenInclusive(60, 120);
        }
    }

    protected float meleeBias() {
        return Mth.clampedLerp(meleeBiasMin, meleeBiasMax, mob.getHealth() / mob.getMaxHealth());
    }

    @Override
    protected void doMovement(double distanceSquared) {
        if (!wantsToMelee) {
            super.doMovement(distanceSquared);
            return;
        }
        if (target.isDeadOrDying()) {
            this.mob.getNavigation().stop();
        } else {
            mob.lookAt(target, 30, 30);
            float strafeForwards = 0;
            float speed = (float) movementSpeed();
            if (distanceSquared > meleeRange * meleeRange) {
                if (mob.tickCount % 5 == 0) {
                    this.mob.getNavigation().moveTo(this.target, meleeMoveSpeedModifier);
                }
                //move control is for localized and simple maneuvers. Navigation is for pathfinding.
                mob.getMoveControl().strafe(0, 0);
            } else {
                this.mob.getNavigation().stop();
                strafeForwards = .25f * meleeMoveSpeedModifier * (4 * distanceSquared > meleeRange * meleeRange ? 1.5f : -1);
                //we do a little strafing
                if (++strafeTime > 25) {
                    if (mob.getRandom().nextDouble() < .1) {
                        strafingClockwise = !strafingClockwise;
                        strafeTime = 0;
                    }
                }
                float strafeDir = strafingClockwise ? 1f : -1f;
                mob.getMoveControl().strafe(strafeForwards, speed * strafeDir);
            }
            //helps with head alignment? for some reason mobs just cannot align their head and body and target for their fucking life
            mob.getLookControl().setLookAt(target);
        }
    }

    @Override
    protected void handleAttackLogic(double distanceSquared) {
        if (!wantsToMelee || distanceSquared > meleeRange * meleeRange || spellCastingMob.isCasting()) {
            super.handleAttackLogic(distanceSquared);
        } else if (--this.attackTime == 0) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            doMeleeAction();
        }

    }

    protected void doMeleeAction() {
        double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        this.mob.doHurtTarget(target);
        resetAttackTimer(distanceSquared);
    }

    public WarlockAttackGoal setMeleeBias(float meleeBiasMin, float meleeBiasMax) {
        this.meleeBiasMin = meleeBiasMin;
        this.meleeBiasMax = meleeBiasMax;
        return this;
    }

    @Override
    public WarlockAttackGoal setSpells(List<AbstractSpell> attackSpells, List<AbstractSpell> defenseSpells, List<AbstractSpell> movementSpells, List<AbstractSpell> supportSpells) {
        return (WarlockAttackGoal) super.setSpells(attackSpells, defenseSpells, movementSpells, supportSpells);
    }

    @Override
    public WarlockAttackGoal setSpellQuality(float minSpellQuality, float maxSpellQuality) {
        return (WarlockAttackGoal) super.setSpellQuality(minSpellQuality, maxSpellQuality);
    }

    @Override
    public WarlockAttackGoal setSingleUseSpell(AbstractSpell spellType, int minDelay, int maxDelay, int minLevel, int maxLevel) {
        return (WarlockAttackGoal) super.setSingleUseSpell(spellType, minDelay, maxDelay, minLevel, maxLevel);
    }

    @Override
    public WarlockAttackGoal setIsFlying() {
        return (WarlockAttackGoal) super.setIsFlying();
    }

    public WarlockAttackGoal setMeleeMovespeedModifier(float meleeMovespeedModifier) {
        this.meleeMoveSpeedModifier = meleeMovespeedModifier;
        return this;
    }

    public WarlockAttackGoal setMeleeAttackInverval(int min, int max) {
        this.meleeAttackIntervalMax = max;
        this.meleeAttackIntervalMin = min;
        return this;
    }

    @Override
    protected double movementSpeed() {
        return wantsToMelee ? meleeMoveSpeedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2 : super.movementSpeed();
    }

    @Override
    protected void resetAttackTimer(double distanceSquared) {
        if (!wantsToMelee || distanceSquared > meleeRange * meleeRange || spellCastingMob.isCasting()) {
            super.resetAttackTimer(distanceSquared);
        } else {
            float f = (float) Math.sqrt(distanceSquared) / this.attackRadius;
            this.attackTime = Mth.floor(f * (float) (this.meleeAttackIntervalMax - this.meleeAttackIntervalMin) + (float) this.meleeAttackIntervalMin);
        }
    }
}

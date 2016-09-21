package org.optaplanner.examples.projectjobscheduling.core.heuristic.selector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.optaplanner.core.config.heuristic.policy.HeuristicConfigPolicy;
import org.optaplanner.core.config.heuristic.selector.common.SelectionCacheType;
import org.optaplanner.core.config.heuristic.selector.common.SelectionOrder;
import org.optaplanner.core.config.heuristic.selector.entity.EntitySelectorConfig;
import org.optaplanner.core.config.heuristic.selector.move.MoveSelectorConfig;
import org.optaplanner.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import org.optaplanner.core.config.heuristic.selector.value.ValueSelectorConfig;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;
import org.optaplanner.core.impl.heuristic.selector.move.MoveSelector;
import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMoveSelector;
import org.optaplanner.core.impl.heuristic.selector.value.ValueSelector;

//@XStreamAlias("resourcesRepulsionMoveSelector")
public class ResourcesRepulsionMoveSelectorConfig extends MoveSelectorConfig<ResourcesRepulsionMoveSelectorConfig> {

    //@XStreamAlias("entitySelector")
    private EntitySelectorConfig entitySelectorConfig = null;
    //@XStreamAlias("valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

    public ResourcesRepulsionMoveSelectorConfig() {
    }

    public EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public MoveSelector buildBaseMoveSelector(HeuristicConfigPolicy configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        if (entitySelectorConfig == null) {
            throw new IllegalStateException("The entitySelectorConfig (" + entitySelectorConfig
                    + ") should haven been initialized during unfolding.");
        }
        EntitySelector entitySelector = entitySelectorConfig.buildEntitySelector(configPolicy,
                minimumCacheType, SelectionOrder.fromRandomSelectionBoolean(randomSelection));
        if (valueSelectorConfig == null) {
            throw new IllegalStateException("The valueSelectorConfig (" + valueSelectorConfig
                    + ") should haven been initialized during unfolding.");
        }
        ValueSelector valueSelector = valueSelectorConfig.buildValueSelector(configPolicy,
                entitySelector.getEntityDescriptor(),
                minimumCacheType, SelectionOrder.fromRandomSelectionBoolean(randomSelection));
        return new ResourcesRepulsionMoveSelector(entitySelector, valueSelector, randomSelection);
    }

    @Override
    protected MoveSelectorConfig buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy configPolicy) {
        Collection<EntityDescriptor> entityDescriptors;
        EntityDescriptor onlyEntityDescriptor = entitySelectorConfig == null ? null
                : entitySelectorConfig.extractEntityDescriptor(configPolicy);
        if (onlyEntityDescriptor != null) {
            entityDescriptors = Collections.singletonList(onlyEntityDescriptor);
        } else {
            entityDescriptors = configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
        }
        List<GenuineVariableDescriptor> variableDescriptorList = new ArrayList<GenuineVariableDescriptor>();
        for (EntityDescriptor entityDescriptor : entityDescriptors) {
            GenuineVariableDescriptor onlyVariableDescriptor = valueSelectorConfig == null ? null
                    : valueSelectorConfig.extractVariableDescriptor(configPolicy, entityDescriptor);
            if (onlyVariableDescriptor != null) {
                if (onlyEntityDescriptor != null) {
                    // No need for unfolding or deducing
                    return null;
                }
                variableDescriptorList.add(onlyVariableDescriptor);
            } else {
                variableDescriptorList.addAll(entityDescriptor.getGenuineVariableDescriptors());
            }
        }
        return buildUnfoldedMoveSelectorConfig(variableDescriptorList);
    }

    protected MoveSelectorConfig buildUnfoldedMoveSelectorConfig(
            List<GenuineVariableDescriptor> variableDescriptorList) {
        List<MoveSelectorConfig> moveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
        for (GenuineVariableDescriptor variableDescriptor : variableDescriptorList) {
            // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
            ResourcesRepulsionMoveSelectorConfig childMoveSelectorConfig = new ResourcesRepulsionMoveSelectorConfig();
            EntitySelectorConfig childEntitySelectorConfig = new EntitySelectorConfig(entitySelectorConfig);
            if (childEntitySelectorConfig.getMimicSelectorRef() == null) {
                childEntitySelectorConfig.setEntityClass(variableDescriptor.getEntityDescriptor().getEntityClass());
            }
            childMoveSelectorConfig.setEntitySelectorConfig(childEntitySelectorConfig);
            ValueSelectorConfig childValueSelectorConfig = new ValueSelectorConfig(valueSelectorConfig);
            childValueSelectorConfig.setVariableName(variableDescriptor.getVariableName());
            childMoveSelectorConfig.setValueSelectorConfig(childValueSelectorConfig);
            moveSelectorConfigList.add(childMoveSelectorConfig);
        }

        MoveSelectorConfig unfoldedMoveSelectorConfig;
        if (moveSelectorConfigList.size() == 1) {
            unfoldedMoveSelectorConfig = moveSelectorConfigList.get(0);
        } else {
            unfoldedMoveSelectorConfig = new UnionMoveSelectorConfig(moveSelectorConfigList);
        }
        unfoldedMoveSelectorConfig.inheritFolded(this);
        return unfoldedMoveSelectorConfig;
    }

    public void inherit(ResourcesRepulsionMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelectorConfig + ", " + valueSelectorConfig + ")";
    }

}
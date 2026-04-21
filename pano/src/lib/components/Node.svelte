<script lang="ts">
    import {
        Handle,
        Position,
        useUpdateNodeInternals,
        type NodeProps,
    } from "@xyflow/svelte";

    import { onDestroy, onMount, untrack } from "svelte";
    import { Plugin } from "$lib/plugin.svelte";
    import { graph, type Node } from "$lib/graph.svelte";

    let { id, data }: NodeProps = $props();
    let node: Node = data.node as Node;

    let builder = Plugin.get(node.manifest as Plugin.Manifest);
    let instance: Plugin.Instance | undefined = $state();
    let container = $state();

    const updateInternals = useUpdateNodeInternals();

    onMount(async () => {
        instance = new (await builder)();
        instance?.mount({
            target: container,
            props: {
                node: node,
            },
        });

        updateInternals(id);
    });

    onDestroy(() => {
        if (instance) {
            instance.unmount();
        }
    });

    let ports = $derived.by(() => {
        let sourceLength = node.ports.filter((p) => p.type == "source").length;
        let targetLength = node.ports.length - sourceLength;

        let sourceStep = 100 / (sourceLength + 1);
        let targetStep = 100 / (targetLength + 1);
        let sourceOffset = sourceStep;
        let targetOffset = targetStep;

        let derived = [];

        for (var i = 0; i < node.ports.length; ++i) {
            switch (node.ports[i].type) {
                case "source":
                    derived.push({
                        id: i,
                        type: "source",
                        position: Position.Bottom,
                        offset: sourceOffset,
                    });

                    sourceOffset += sourceStep;

                    break;
                case "target":
                    derived.push({
                        id: i,
                        type: "target",
                        position: Position.Top,
                        offset: targetOffset,
                    });

                    targetOffset += targetStep;

                    break;
            }
        }

        untrack(() => {
            updateInternals(id);
            // graph.onPortsChanged(Number(id));
        });

        return derived;
    });
</script>

<div
    class="px-4 py-6 shadow-md card bg-base-100 min-w-60"
    bind:this={container}
></div>

{#each ports as port}
    <Handle
        id={`${port.id}`}
        type={port.type as any}
        style="background-color: rgba(0, 0, 0, 0); width: 30px; height: 30px; left: {port.offset}%; border: none;"
        position={port.position}
        ><span class="badge badge-soft badge-primary">{port.id}</span></Handle
    >
{/each}

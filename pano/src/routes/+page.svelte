<script lang="ts">
    import { Plugin } from "$lib/plugin.svelte";
    import Node from "$lib/components/Node.svelte";
    import {
        type Node as FlowNode,
        type Edge as FlowLink,
        Background,
        SvelteFlow,
        type Connection,
    } from "@xyflow/svelte";
    import "@xyflow/svelte/dist/style.css";
    import { graph } from "$lib/graph.svelte";
    import { NavigationMenu, ContextMenu } from "bits-ui";

    import { CircleAlert, CloudAlert, CloudCheck, Disc3 } from "@lucide/svelte";
    import { onMount } from "svelte";

    let sync: Promise<boolean> = $state(Promise.resolve(false));
    let ref = $state<HTMLDivElement | null>(null);
    let manifests = $state<Record<string, Plugin.Manifest>>({});

    let nodes: FlowNode[] = $derived.by(() => {
        return graph.nodes.map((node) => ({
            id: `${node.id}`,
            type: "Node",
            position: node.position,
            data: {
                node: node,
            },
        }));
    });

    let edges: FlowLink[] = $derived.by(() => {
        return graph.links.map((link) => ({
            id: `${link.id}`,
            source: `${link.source.node}`,
            sourceHandle: `${link.source.port}`,
            target: `${link.target.node}`,
            targetHandle: `${link.target.port}`,
        }));
    });

    onMount(async () => {
        sync = graph.sync();
        manifests = await Plugin.list();
    });
</script>

<NavigationMenu.Root class="flex-none max-w-none p-4">
    <NavigationMenu.List class="flex items-center">
        <NavigationMenu.Item>
            <NavigationMenu.Link href="#">Home</NavigationMenu.Link>
        </NavigationMenu.Item>
        <NavigationMenu.Item class="ms-auto">
            {#await sync}
                <button class="btn btn-info">
                    <Disc3 class="animate-spin" />
                </button>
            {:then status}
                {#if status}
                    {#if graph.dirty}
                        <button
                            class="btn btn-warning"
                            onclick={() => {
                                sync = graph.sync();
                            }}
                        >
                            <CloudAlert />
                        </button>
                    {:else}
                        <button
                            class="btn btn-success"
                            onclick={() => {
                                sync = graph.sync();
                            }}
                        >
                            <CloudCheck />
                        </button>
                    {/if}
                {:else}
                    <button
                        class="btn btn-error"
                        onclick={() => {
                            sync = graph.sync();
                        }}
                    >
                        <CircleAlert />
                    </button>
                {/if}
            {/await}
        </NavigationMenu.Item>
    </NavigationMenu.List>
</NavigationMenu.Root>

<ContextMenu.Root>
    <ContextMenu.Trigger class="flex-auto">
        <SvelteFlow
            bind:nodes
            bind:edges
            nodeTypes={{ Node: Node }}
            defaultEdgeOptions={{
                type: "smoothstep",
                animated: true,
            }}
            onconnect={(connection: Connection) => {
                graph.onConnect(connection);
            }}
            ondelete={(params: any) => {
                graph.onDelete(params);
            }}
            onnodedragstop={(data) => {
                graph.onNodeDrag(data);
            }}
        >
            <Background />
        </SvelteFlow>
    </ContextMenu.Trigger>
    <ContextMenu.Content
        class="bg-base-100 shadow-sm flex flex-col gap-2 p-4 card"
        bind:ref
    >
        <ContextMenu.Sub>
            <ContextMenu.SubTrigger class="btn">Add</ContextMenu.SubTrigger>
            <ContextMenu.SubContent
                class="bg-base-100 shadow-sm flex flex-col gap-2 p-4 card"
            >
                {#each Object.values(manifests) as manifest}
                    <ContextMenu.Item>
                        <button class="btn" onclick={() => graph.add(manifest)}
                            >{manifest.name}</button
                        >
                    </ContextMenu.Item>
                {/each}
            </ContextMenu.SubContent>
        </ContextMenu.Sub>
    </ContextMenu.Content>
</ContextMenu.Root>

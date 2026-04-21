<script lang="ts">
    import { onMount } from "svelte";
    import Plugin from "../src/index";

    type Port = {
        type: string;
        schema: string | undefined;
    };

    class Node {
        configuration: any = {};
        ports: Port[] = [];

        sync(): void {}
        update(data: any): void {}
        monitor(): Promise<any[] | null> {
            return Promise.resolve(null);
        }
    }

    let plug = new Plugin();
    let node = new Node();
    let container = $state();

    onMount(() => {
        plug.mount({
            target: container,
            props: {
                node: node,
            },
        });
    });
</script>

<div class="w-100 m-10 p-4 shadow-sm card" bind:this={container}></div>

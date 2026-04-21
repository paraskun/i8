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

        async monitor(): Promise<any[] | null> {
            return await new Promise((resolve) => {
                const id = setTimeout(() => {
                    clearTimeout(id);
                    resolve([{ text: "Hi." }, { text: "Bye." }]);
                }, 1000);
            });
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

<div class="w-60 m-10 p-4 shadow-sm card" bind:this={container}></div>

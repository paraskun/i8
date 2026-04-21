import { mount as $mount, unmount as $unmount } from 'svelte'
import Component from './Node.svelte';

export default class Node {
    mnt: any;

    mount(options: any) {
        this.mnt = $mount(Component, options);
    }

    unmount(options: any) {
        $unmount(this.mnt, options);
    }
};

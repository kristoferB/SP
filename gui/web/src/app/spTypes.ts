export interface HierarchyRoot extends Item {
    children: Array<HierarchyNode>;
}

export interface HierarchyNode {
    item: string;
    children: Array<HierarchyNode>;
    id: string;
}

export interface Item {
    name: string;
    attributes: Object;
    id: string;
}

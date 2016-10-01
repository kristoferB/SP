export class Menu {
  text: string;
  symbol: string;
  buttons: MenuButton[];
}

export class MenuButton {
  action: () => void;
  text: string;
  symbol: string;
}

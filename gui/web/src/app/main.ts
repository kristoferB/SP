import { upgAdapter } from './upg-helpers/upg-adapter';
import { upgConvertStuff } from './upg-helpers/upg-convert-stuff';

upgConvertStuff(upgAdapter);

upgAdapter.bootstrap(document.documentElement, ['app']);

"use strict";
var upg_adapter_1 = require('./upg-helpers/upg-adapter');
var upg_convert_stuff_1 = require('./upg-helpers/upg-convert-stuff');
upg_convert_stuff_1.upgConvertStuff(upg_adapter_1.upgAdapter);
upg_adapter_1.upgAdapter.bootstrap(document.documentElement, ['app']);
//# sourceMappingURL=main.js.map
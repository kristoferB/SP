(function () {
    'use strict';

    angular
        .module('app.settings')
        .factory('settingsService', settingsService);

    settingsService.$inject = ['$localStorage'];
    /* @ngInject */
    function settingsService($localStorage) {
        $localStorage.$reset();

        var color_presets = getColorPresets();
        var layout_presets = getLayoutPresets();
    
        var service = {
            storage: $localStorage.$default({
                saved_color_themes: color_presets,
                saved_layout_themes: layout_presets,

                current_color_theme: 'default_white',
                current_layout_theme: 'standard',

                visuals: {
                    colors: color_presets.current_color_theme,
                    layout: layout_presets.current_layout_theme
                },

                gridlock: false,
                navbar_shown: true
            }),
        
            color_themes: ['default_white', 'blue', 'dark'],
            layout_themes: ['standard', 'compact'],

            theme_refreshed: theme_refreshed,
            getLayoutTheme: getLayoutTheme,
            getColorTheme: getColorTheme
        };

        activate();

        return service;

        function activate() {
            theme_refreshed();
        }
        function theme_refreshed(){
            less.modifyVars(
                Object.assign(
                    getColorTheme(),
                    getLayoutTheme()
                )
            );
        }

        function getColorTheme(){
            return $localStorage.saved_color_themes[ $localStorage.current_color_theme]
        }

        function getLayoutTheme(){
            return $localStorage.saved_layout_themes[$localStorage.current_layout_theme]
        }

        //TODO put these somewhere far away from the logic
        function getColorPresets(){
            return {
                default_white: 0,
                blue: {
                    body_color: '#58666e',
                    body_background: '#476888',

                    // h1, h2, etc.
                    header_color: '#black',

                    // jstree
                    jstree_color: 'black',
                    jstree_background: 'blanchedalmond',

                    // .view-header
                    view_header_color: '#58666e',
                    view_header_background: '#f6f8f8',
                    
                    // a
                    a_color: '#333',
                    a_hover_color: '#888',

                    // dialogs
                    dialog_header_confirm_h_color: '#777',
                    dialog_header_confirm_span_color: '#777',

                    // gridster
                    gridster_panel_background: '#f5f5f5',
                    gridster_content_background: 'blanchedalmond',
                    gridster_panel_heading_background: '#f5f5f5',

                    // buttons
                    button_default_color: '#58666e',
                    button_default_background: '#fff',
                    button_default_border_color: '#dee5e7',
                    button_default_border_bottom_color: '#d8e1e3',
                    button_default_background_hover: '#edf1f2',
                    button_default_border_color_hover: '#c7d3d6',

                    button_primary_color: '#fff',
                    button_primary_background: '#7266ba',
                    button_primary_color_hover: '#fff',
                    button_primary_background_hover: '#6254b2',
                    button_primary_border_hover: '#5a4daa',

                    button_info_background: '#52b9e9',
                    button_info_background_hover: '#459fc9',

                    button_success_background: '#43c83c',
                    button_success_background_hover: '#36a530',

                    button_warning_background: '#f88529',
                    button_warning_background_hover: '#d67323',

                    button_danger_background: '#fa3031',
                    button_danger_background_hover: '#d82829',

                    // forms
                    input_group_addon_color: '#58666e',
                    input_group_addon_background: '#fff',
                    form_control_border_color: '#dee5e7',

                    // dropdown menu
                    dropdown_menu_background: '#aaaaaa',
                    dropdown_menu_background_hover: '#edf1f2',
                    dropdown_menu_background_divider: '#e5e5e5',

                    // navbar 
                    navbar_background: '#000000',
                    navbar_color: '#ffffff',
                    // this is the SP logo orange
                    navbar_header_background: '#de691c',
                    // SP logo foreground white
                    navbar_brand_title_color: '#fff',

                    // pagination
                    pagination_color: '#666',
                    pagination_background: '#fff',
                    pagination_color_hover: '#333',
                    pagination_background_hover: '#fafafa'
                },
                dark: {
                    body_color: '#140253',
                    body_background: 'darkgray',

                    // h1, h2, etc.
                    header_color: '#140253',

                    // jstree
                    jstree_color: 'gray',
                    jstree_background: 'black',

                    // .view-header
                    view_header_color: '#58666e',
                    view_header_background: '#f6f8f8',
                    
                    // a
                    a_color: '#4870f9',
                    a_hover_color: '#4e9fe3',

                    // dialogs
                    dialog_header_confirm_h_color: '#777',
                    dialog_header_confirm_span_color: '#777',

                    // gridster
                    gridster_panel_background: '#888888',
                    gridster_content_background: 'black',
                    gridster_panel_heading_background: '#cccccc',

                    // buttons
                    button_default_color: '#58666e',
                    button_default_background: '#fff',
                    button_default_border_color: '#dee5e7',
                    button_default_border_bottom_color: '#d8e1e3',
                    button_default_background_hover: '#edf1f2',
                    button_default_border_color_hover: '#c7d3d6',

                    button_primary_color: '#fff',
                    button_primary_background: '#bb37b3',
                    button_primary_color_hover: '#fff',
                    button_primary_background_hover: '#6254b2',
                    button_primary_border_hover: '#5a4daa',

                    button_info_background: '#7de5ed',
                    button_info_background_hover: '#459fc9',

                    button_success_background: '#64e387',
                    button_success_background_hover: '#36a530',

                    button_warning_background: '#a265a1',
                    button_warning_background_hover: '#d67323',

                    button_danger_background: '#344262',
                    button_danger_background_hover: '#d82829',

                    // forms
                    input_group_addon_color: '#58666e',
                    input_group_addon_background: '#fff',
                    form_control_border_color: '#dee5e7',

                    // dropdown menu
                    dropdown_menu_background: '#fff',
                    dropdown_menu_background_hover: '#edf1f2',
                    dropdown_menu_background_divider: '#e5e5e5',

                    // navbar 
                    navbar_background: '#000000',
                    navbar_color: '#ffffff',
                    // this is the SP logo orange
                    navbar_header_background: '#de691c',
                    // SP logo foreground white
                    navbar_brand_title_color: '#fff',

                    // pagination
                    pagination_color: '#666',
                    pagination_background: '#fff',
                    pagination_color_hover: '#333',
                    pagination_background_hover: '#fafafa'
                }
            }
        }
        function getLayoutPresets(){
            return {
                standard: {
                    gridster_panel_margin: 10,

                    body_padding: '20px', 
                    gridster_panel_border_radius:          '10px',
                    gridster_panel_toolbar_padding:        '15px 16px', 
                    gridster_panel_content_margin_left:    '15px', 
                    gridster_panel_content_margin_right:   '15px',
                    gridster_panel_content_margin_bottom:  '15px',
                    gridster_panel_content_border:         '1px'

                },
                compact: {
                    gridster_panel_margin: 0,

                    body_padding: '0px', 
                    gridster_panel_border_radius:          '0px',
                    gridster_panel_toolbar_padding:        '1px 3px', 
                    gridster_panel_content_margin_left:    '3px', 
                    gridster_panel_content_margin_right:   '3px',
                    gridster_panel_content_margin_bottom:  '3px',
                    gridster_panel_content_border:         '1px'
                }
            }
        }
    }
})();


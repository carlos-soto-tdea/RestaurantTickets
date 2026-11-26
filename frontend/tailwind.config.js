/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          'primary-700': '#14532D',   // Verde nopal oscuro (acción principal, encabezados)
          'primary-500': '#22C55E',   // Verde fresco (indicadores activos)
          'secondary-700': '#B91C1C', // Rojo chile (errores, destructivo, marca secundaria)
          'accent-500': '#F59E0B',    // Maíz / ámbar (llamados de atención, pedido listo)
          'accent-200': '#FDE68A',    // Maíz claro (advertencia suave)
          background: '#FFF8ED',      // Crema tortilla (fondo general cálido)
          surface: '#FFFFFF',         // Superficies de contenido
          border: '#D6D3D1',          // Bordes y divisores
          text: '#1F2937',            // Texto principal
          'text-muted': '#57534E',    // Texto secundario
        },
        semantic: {
          'success-bg': '#DCFCE7',
          'success-text': '#166534',
          'info-bg': '#DBEAFE',
          'info-text': '#1E40AF',
          'warning-bg': '#FEF3C7',
          'warning-text': '#92400E',
          'error-bg': '#FEE2E2',
          'error-text': '#991B1B',
          'neutral-bg': '#F5F5F4',
          'neutral-text': '#44403C',
        },
      },
    },
  },
  plugins: [],
};

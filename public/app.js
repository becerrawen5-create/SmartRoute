/**
 * SmartRoute AI — Controlador de Interacciones UI
 * Manejo del Acordeón y Notificación Snackbar estilo Android
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. Elementos del DOM
  const downloadBtn = document.getElementById('downloadBtn');
  const accordionToggle = document.getElementById('accordionToggle');
  const accordionContent = document.getElementById('accordionContent');
  const snackbar = document.getElementById('snackbar');
  const snackbarAction = document.getElementById('snackbarAction');

  let snackbarTimeout = null;

  // 2. Acordeón de Instalación (Guía de 3 pasos)
  if (accordionToggle && accordionContent) {
    accordionToggle.addEventListener('click', () => {
      const isExpanded = accordionToggle.getAttribute('aria-expanded') === 'true';
      const nextState = !isExpanded;

      accordionToggle.setAttribute('aria-expanded', String(nextState));
      
      if (nextState) {
        accordionContent.removeAttribute('hidden');
      } else {
        accordionContent.setAttribute('hidden', '');
      }
    });
  }

  // 3. Función para mostrar Snackbar nativo de Android
  function showAndroidSnackbar() {
    if (!snackbar) return;

    if (snackbarTimeout) {
      clearTimeout(snackbarTimeout);
    }

    snackbar.classList.add('show');

    // Auto-cierre tras 4.5 segundos (comportamiento estándar de Android Snackbar LONG)
    snackbarTimeout = setTimeout(() => {
      hideSnackbar();
    }, 4500);
  }

  function hideSnackbar() {
    if (!snackbar) return;
    snackbar.classList.remove('show');
    if (snackbarTimeout) {
      clearTimeout(snackbarTimeout);
      snackbarTimeout = null;
    }
  }

  // 4. Evento de Clic en Descarga
  if (downloadBtn) {
    downloadBtn.addEventListener('click', (e) => {
      // Disparar la notificación nativa
      showAndroidSnackbar();

      // Efecto táctil sutil
      downloadBtn.style.transform = 'scale(0.98)';
      setTimeout(() => {
        downloadBtn.style.transform = '';
      }, 150);
    });
  }

  // 5. Botón de acción para descartar el Snackbar
  if (snackbarAction) {
    snackbarAction.addEventListener('click', () => {
      hideSnackbar();
    });
  }
});

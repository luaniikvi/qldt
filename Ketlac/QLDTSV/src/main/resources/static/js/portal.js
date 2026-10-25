/**
 * HCMUTE Portal Client Interactions & Dashboard Carousel
 * Complies with Relate Motion Tokens & Accessibility Standards
 */

document.addEventListener('DOMContentLoaded', function () {
  // 1. Mobile Sidebar Toggle
  initMobileSidebar();

  // 2. Client-side Search Filter for Tables (Image 2 Parity)
  initTableSearch();

  // 3. Password Visibility Toggle
  initPasswordToggle();

  // 4. Form Submit Double-Click Protection
  initFormSubmitProtection();

  // 5. Dashboard Hero Carousel
  initDashboardCarousel();

  // 6. Sidebar Scroll Position Preservation
  initSidebarScrollRestoration();
});

/**
 * Mobile Sidebar Drawer Controller
 */
function initMobileSidebar() {
  const sidebarToggleBtn = document.getElementById('btnMobileSidebar');
  const sidebar = document.getElementById('portalSidebar');
  const backdrop = document.getElementById('portalBackdrop');

  if (sidebarToggleBtn && sidebar && backdrop) {
    sidebarToggleBtn.addEventListener('click', function () {
      sidebar.classList.toggle('show');
      backdrop.classList.toggle('show');
      sidebarToggleBtn.setAttribute('aria-expanded', sidebar.classList.contains('show'));
    });

    backdrop.addEventListener('click', function () {
      sidebar.classList.remove('show');
      backdrop.classList.remove('show');
      sidebarToggleBtn.setAttribute('aria-expanded', 'false');
    });
  }
}

/**
 * Real-time table search filter with empty state feedback
 */
function initTableSearch() {
  const searchInput = document.getElementById('tableSearchInput');
  if (!searchInput) return;

  searchInput.addEventListener('input', function () {
    const filter = this.value.toLowerCase().trim();
    const targetTable = document.querySelector('.hcmute-table tbody');
    if (!targetTable) return;

    const rows = targetTable.querySelectorAll('tr:not(.empty-row)');
    let matchCount = 0;

    rows.forEach(function (row) {
      const text = row.textContent.toLowerCase();
      if (text.includes(filter)) {
        row.style.display = '';
        matchCount++;
      } else {
        row.style.display = 'none';
      }
    });

    let noResultRow = document.getElementById('noSearchMatchRow');
    if (matchCount === 0 && filter !== '') {
      if (!noResultRow) {
        noResultRow = document.createElement('tr');
        noResultRow.id = 'noSearchMatchRow';
        noResultRow.className = 'empty-row';
        const colCount = rows[0] ? rows[0].cells.length : 4;
        noResultRow.innerHTML = `<td colspan="${colCount}" class="text-center py-4 text-muted">
          <i class="bi bi-search me-1"></i> Không tìm thấy kết quả phù hợp với từ khóa "<strong>${escapeHtml(filter)}</strong>"
        </td>`;
        targetTable.appendChild(noResultRow);
      } else {
        noResultRow.style.display = '';
        noResultRow.querySelector('strong').textContent = filter;
      }
    } else if (noResultRow) {
      noResultRow.style.display = 'none';
    }
  });
}

/**
 * Toggle password reveal
 */
function initPasswordToggle() {
  document.querySelectorAll('.btn-toggle-password').forEach(function (btn) {
    btn.addEventListener('click', function () {
      const targetInput = document.querySelector(this.getAttribute('data-target'));
      if (targetInput) {
        const isPassword = targetInput.type === 'password';
        targetInput.type = isPassword ? 'text' : 'password';
        const icon = this.querySelector('i');
        if (icon) {
          icon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
        }
      }
    });
  });
}

/**
 * Form Submit Protection
 * - Chặn submit lặp (disable nút + spinner) CHỈ khi form thực sự được gửi đi.
 * - Hộp thoại xác nhận lấy từ thuộc tính data-confirm của <form>; nếu người dùng Cancel thì
 *   preventDefault() và KHÔNG đổi trạng thái nút (tránh kẹt "Đang xử lý...").
 */
function initFormSubmitProtection() {
  document.querySelectorAll('form:not([data-no-disable])').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      if (event.defaultPrevented) return;

      const message = form.getAttribute('data-confirm');
      if (message && !window.confirm(message)) {
        event.preventDefault();
        return;
      }

      const submitBtn = form.querySelector('button[type="submit"]:not(.no-spin)');
      if (submitBtn && !submitBtn.disabled) {
        submitBtn.dataset.originalHtml = submitBtn.innerHTML;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...';
      }
    });
  });

  window.addEventListener('pageshow', function (event) {
    if (!event.persisted) return;
    document.querySelectorAll('button[data-original-html]').forEach(function (btn) {
      btn.disabled = false;
      btn.innerHTML = btn.dataset.originalHtml;
      delete btn.dataset.originalHtml;
    });
  });
}

/**
 * Dashboard Banner Carousel Controller
 * Features:
 * - Next/prev buttons, dots, counter indicator (1/4)
 * - 6s auto-slide with play/pause
 * - Pauses on hover, focus, tab hidden (visibilitychange)
 * - Touch swipe (horizontal without blocking vertical scroll)
 * - Keyboard navigation (Left/Right arrows)
 * - Prefers-reduced-motion compliance
 */
function initDashboardCarousel() {
  const carousel = document.querySelector('.dashboard-carousel-wrapper');
  if (!carousel) return;

  const track = carousel.querySelector('.carousel-slides-track');
  const slides = carousel.querySelectorAll('.carousel-slide');
  const btnPrev = carousel.querySelector('.carousel-btn-prev');
  const btnNext = carousel.querySelector('.carousel-btn-next');
  const btnTogglePlay = carousel.querySelector('.carousel-btn-play');
  const counter = carousel.querySelector('.carousel-counter');
  const dotsContainer = carousel.querySelector('.carousel-dots');

  if (!track || slides.length === 0) return;

  const totalSlides = slides.length;
  let currentIndex = 0;
  let autoPlayTimer = null;
  const autoPlayDelay = 6000; // 6 seconds

  // Check prefers-reduced-motion
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  let isPlaying = !prefersReducedMotion && totalSlides > 1;
  let isManuallyPaused = prefersReducedMotion;

  // Single slide handling
  if (totalSlides <= 1) {
    if (btnPrev) btnPrev.style.display = 'none';
    if (btnNext) btnNext.style.display = 'none';
    if (btnTogglePlay) btnTogglePlay.style.display = 'none';
    if (counter) counter.style.display = 'none';
    if (dotsContainer) dotsContainer.style.display = 'none';
    return;
  }

  // Render dots if empty
  if (dotsContainer && dotsContainer.children.length === 0) {
    for (let i = 0; i < totalSlides; i++) {
      const dot = document.createElement('button');
      dot.className = 'carousel-dot' + (i === 0 ? ' active' : '');
      dot.setAttribute('type', 'button');
      dot.setAttribute('aria-label', 'Chuyển đến ảnh ' + (i + 1));
      dot.addEventListener('click', function () {
        goToSlide(i);
      });
      dotsContainer.appendChild(dot);
    }
  }

  function updateSlidePosition() {
    track.style.transform = `translateX(-${currentIndex * 100}%)`;

    // Update Counter
    if (counter) {
      counter.textContent = `${currentIndex + 1}/${totalSlides}`;
    }

    // Update Dots
    if (dotsContainer) {
      const dots = dotsContainer.querySelectorAll('.carousel-dot');
      dots.forEach((dot, index) => {
        dot.classList.toggle('active', index === currentIndex);
      });
    }

    // Update Accessibility attributes
    slides.forEach((slide, index) => {
      const isActive = index === currentIndex;
      slide.setAttribute('aria-hidden', !isActive);
      // Disable focus on interactive elements inside hidden slides
      slide.querySelectorAll('a, button').forEach(el => {
        if (isActive) {
          el.removeAttribute('tabindex');
        } else {
          el.setAttribute('tabindex', '-1');
        }
      });
    });
  }

  function goToSlide(index) {
    currentIndex = (index + totalSlides) % totalSlides;
    updateSlidePosition();
  }

  function nextSlide() {
    goToSlide(currentIndex + 1);
  }

  function prevSlide() {
    goToSlide(currentIndex - 1);
  }

  function startAutoPlay() {
    if (isManuallyPaused || prefersReducedMotion) return;
    stopAutoPlay();
    autoPlayTimer = setInterval(nextSlide, autoPlayDelay);
    isPlaying = true;
    updatePlayPauseButton();
  }

  function stopAutoPlay() {
    if (autoPlayTimer) {
      clearInterval(autoPlayTimer);
      autoPlayTimer = null;
    }
    isPlaying = false;
    updatePlayPauseButton();
  }

  function updatePlayPauseButton() {
    if (!btnTogglePlay) return;
    const icon = btnTogglePlay.querySelector('i');
    if (isPlaying) {
      btnTogglePlay.setAttribute('aria-label', 'Tạm dừng chạy tự động');
      btnTogglePlay.setAttribute('title', 'Tạm dừng chạy');
      if (icon) icon.className = 'bi bi-pause-fill';
    } else {
      btnTogglePlay.setAttribute('aria-label', 'Tiếp tục chạy tự động');
      btnTogglePlay.setAttribute('title', 'Tiếp tục chạy');
      if (icon) icon.className = 'bi bi-play-fill';
    }
  }

  // Prev / Next button listeners
  if (btnPrev) {
    btnPrev.addEventListener('click', function () {
      prevSlide();
    });
  }

  if (btnNext) {
    btnNext.addEventListener('click', function () {
      nextSlide();
    });
  }

  // Play / Pause Toggle button
  if (btnTogglePlay) {
    btnTogglePlay.addEventListener('click', function () {
      if (isPlaying) {
        isManuallyPaused = true;
        stopAutoPlay();
      } else {
        isManuallyPaused = false;
        startAutoPlay();
      }
    });
  }

  // Hover pause / resume (only if not manually paused)
  carousel.addEventListener('mouseenter', function () {
    if (isPlaying && !isManuallyPaused) {
      clearInterval(autoPlayTimer);
      autoPlayTimer = null;
    }
  });

  carousel.addEventListener('mouseleave', function () {
    if (!isManuallyPaused && !prefersReducedMotion) {
      startAutoPlay();
    }
  });

  // Focus within carousel pauses autoplay
  carousel.addEventListener('focusin', function () {
    if (isPlaying && !isManuallyPaused) {
      clearInterval(autoPlayTimer);
      autoPlayTimer = null;
    }
  });

  carousel.addEventListener('focusout', function (e) {
    if (!carousel.contains(e.relatedTarget) && !isManuallyPaused && !prefersReducedMotion) {
      startAutoPlay();
    }
  });

  // Document visibility change pause/resume
  document.addEventListener('visibilitychange', function () {
    if (document.hidden) {
      if (autoPlayTimer) {
        clearInterval(autoPlayTimer);
        autoPlayTimer = null;
      }
    } else {
      if (!isManuallyPaused && !prefersReducedMotion) {
        startAutoPlay();
      }
    }
  });

  // Keyboard Navigation when focus is on carousel
  carousel.setAttribute('tabindex', '0');
  carousel.setAttribute('role', 'region');
  carousel.setAttribute('aria-label', 'Banner hình ảnh trường và hoạt động học thuật');
  carousel.addEventListener('keydown', function (e) {
    if (e.key === 'ArrowLeft') {
      prevSlide();
      e.preventDefault();
    } else if (e.key === 'ArrowRight') {
      nextSlide();
      e.preventDefault();
    }
  });

  // Touch Swipe (without blocking vertical scrolling)
  let touchStartX = 0;
  let touchStartY = 0;
  let isSwiping = false;

  carousel.addEventListener('touchstart', function (e) {
    if (e.touches.length === 1) {
      touchStartX = e.touches[0].clientX;
      touchStartY = e.touches[0].clientY;
      isSwiping = true;
    }
  }, { passive: true });

  carousel.addEventListener('touchmove', function (e) {
    if (!isSwiping || e.touches.length !== 1) return;
    const currentX = e.touches[0].clientX;
    const currentY = e.touches[0].clientY;
    const diffX = touchStartX - currentX;
    const diffY = touchStartY - currentY;

    // If horizontal motion is greater than vertical, handle swipe and prevent horizontal bounce
    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 10) {
      // Intentionally allowing vertical scroll while capturing clear horizontal gesture
    }
  }, { passive: true });

  carousel.addEventListener('touchend', function (e) {
    if (!isSwiping) return;
    isSwiping = false;
    const touchEndX = e.changedTouches[0].clientX;
    const touchEndY = e.changedTouches[0].clientY;
    const diffX = touchStartX - touchEndX;
    const diffY = touchStartY - touchEndY;

    // Threshold of 45px swipe with horizontal dominance
    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 45) {
      if (diffX > 0) {
        nextSlide();
      } else {
        prevSlide();
      }
    }
  }, { passive: true });

  // Initial State Setup
  updateSlidePosition();
  if (isPlaying) {
    startAutoPlay();
  }
}

function escapeHtml(string) {
  return String(string).replace(/[&<>"'`=\/]/g, function (s) {
    return ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
      '/': '&#x2F;',
      '`': '&#x60;',
      '=': '&#x3D;'
    })[s];
  });
}

/**
 * Sidebar Scroll Position Preservation
 * Giữ nguyên vị trí cuộn của sidebar khi chuyển giữa các trang trong portal,
 * đồng thời đảm bảo menu item đang active luôn nằm trong tầm nhìn của người dùng.
 */
function initSidebarScrollRestoration() {
  const sidebar = document.getElementById('portalSidebar');
  if (!sidebar) return;

  // 1. Khôi phục vị trí cuộn đã lưu trong sessionStorage nếu có
  const savedScrollTop = sessionStorage.getItem('portal_sidebar_scroll_top');
  if (savedScrollTop !== null) {
    sidebar.scrollTop = parseInt(savedScrollTop, 10);
  }

  // 2. Tìm mục đang active và đảm bảo nó hiển thị trong khung nhìn sidebar
  const activeItem = sidebar.querySelector('.sidebar-nav-item.active');
  if (activeItem) {
    const sidebarRect = sidebar.getBoundingClientRect();
    const itemRect = activeItem.getBoundingClientRect();

    const isVisible = (
      itemRect.top >= sidebarRect.top + 30 &&
      itemRect.bottom <= sidebarRect.bottom - 30
    );

    if (!isVisible) {
      activeItem.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' });
      // Cập nhật lại vị trí cuộn sau khi scrollIntoView
      sessionStorage.setItem('portal_sidebar_scroll_top', sidebar.scrollTop);
    }
  }

  // 3. Lưu vị trí cuộn khi người dùng cuộn sidebar
  let scrollTimer;
  sidebar.addEventListener('scroll', function () {
    clearTimeout(scrollTimer);
    scrollTimer = setTimeout(function () {
      sessionStorage.setItem('portal_sidebar_scroll_top', sidebar.scrollTop);
    }, 50);
  }, { passive: true });

  // 4. Lưu ngay lập tức khi click vào liên kết bất kỳ trong sidebar
  sidebar.querySelectorAll('a').forEach(function (link) {
    link.addEventListener('click', function () {
      sessionStorage.setItem('portal_sidebar_scroll_top', sidebar.scrollTop);
    });
  });

  // 5. Lưu khi trước khi chuyển trang
  window.addEventListener('beforeunload', function () {
    sessionStorage.setItem('portal_sidebar_scroll_top', sidebar.scrollTop);
  });
  window.addEventListener('pagehide', function () {
    sessionStorage.setItem('portal_sidebar_scroll_top', sidebar.scrollTop);
  });
}


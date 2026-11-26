import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('App skeleton smoke test', () => {
  it('renders El Chupacabras header and self-service subtitle', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: /El Chupacabras/i })).toBeInTheDocument();
    expect(screen.getByText(/Los Patios, Norte de Santander/i)).toBeInTheDocument();
  });
});

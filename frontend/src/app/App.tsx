export function App() {
  return (
    <div className="min-h-screen bg-brand-background text-brand-text flex flex-col">
      <header className="bg-brand-primary-700 text-white px-6 py-4 shadow-sm">
        <h1 className="text-xl font-bold">El Chupacabras — Restaurant Tickets</h1>
      </header>
      <main className="flex-1 p-6">
        <div className="bg-brand-surface border border-brand-border rounded-lg p-6">
          <p className="text-brand-text-muted">
            Sistema operativo de autoservicio — Los Patios, Norte de Santander
          </p>
        </div>
      </main>
    </div>
  );
}

export default App;

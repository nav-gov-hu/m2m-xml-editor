using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;

internal static class Program
{
    private const string PayloadPattern = "M2M-XML-EDITOR-Payload-Setup-*.exe";

    [STAThread]
    private static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new SplashForm());
    }

    private sealed class SplashForm : Form
    {
        private readonly Label statusLabel;

        internal SplashForm()
        {
            Text = "M2M XML EDITOR";
            StartPosition = FormStartPosition.CenterScreen;
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            ShowInTaskbar = true;
            TopMost = true;
            ClientSize = new Size(520, 190);
            BackColor = Color.White;
            Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath);

            var title = new Label
            {
                AutoSize = false,
                Text = "M2M XML EDITOR",
                Font = new Font("Segoe UI", 20F, FontStyle.Bold, GraphicsUnit.Point),
                Location = new Point(28, 24),
                Size = new Size(460, 42),
                TextAlign = ContentAlignment.MiddleLeft
            };

            statusLabel = new Label
            {
                AutoSize = false,
                Text = "A telepítő előkészítése folyamatban...",
                Font = new Font("Segoe UI", 10.5F, FontStyle.Regular, GraphicsUnit.Point),
                Location = new Point(31, 82),
                Size = new Size(455, 26),
                TextAlign = ContentAlignment.MiddleLeft
            };

            var progress = new ProgressBar
            {
                Style = ProgressBarStyle.Marquee,
                MarqueeAnimationSpeed = 25,
                Location = new Point(32, 124),
                Size = new Size(455, 18)
            };

            var waitLabel = new Label
            {
                AutoSize = false,
                Text = "Kérjük, várjon.",
                Font = new Font("Segoe UI", 9F, FontStyle.Regular, GraphicsUnit.Point),
                ForeColor = Color.DimGray,
                Location = new Point(31, 149),
                Size = new Size(455, 22),
                TextAlign = ContentAlignment.MiddleLeft
            };

            Controls.Add(title);
            Controls.Add(statusLabel);
            Controls.Add(progress);
            Controls.Add(waitLabel);

            Shown += async (_, __) => await StartPayloadAsync();
        }

        private async Task StartPayloadAsync()
        {
            await Task.Delay(80);

            try
            {
                string baseDirectory = AppDomain.CurrentDomain.BaseDirectory;
                string payload = Directory.GetFiles(baseDirectory, PayloadPattern)
                    .OrderByDescending(File.GetLastWriteTimeUtc)
                    .FirstOrDefault();

                if (string.IsNullOrEmpty(payload))
                {
                    throw new FileNotFoundException(
                        "A tényleges telepítő nem található. Tartsa a bootstrapper EXE-t, a Payload Setup EXE-t és a BIN fájlokat ugyanabban a mappában.");
                }

                statusLabel.Text = "A telepítő indítása folyamatban...";

                var startInfo = new ProcessStartInfo
                {
                    FileName = payload,
                    Arguments = BuildForwardedArguments(),
                    WorkingDirectory = baseDirectory,
                    UseShellExecute = true
                };

                Process process = Process.Start(startInfo);
                if (process == null)
                {
                    throw new InvalidOperationException("A tényleges telepítő nem indítható el.");
                }

                await Task.Run(() =>
                {
                    try
                    {
                        process.WaitForInputIdle(30000);
                    }
                    catch (InvalidOperationException)
                    {
                        // A folyamat a GUI inicializálása előtt kilépett vagy nem GUI folyamat.
                    }
                });

                await Task.Delay(250);
                Close();
            }
            catch (Exception ex)
            {
                TopMost = false;
                MessageBox.Show(this, ex.Message, "M2M XML EDITOR telepítő",
                    MessageBoxButtons.OK, MessageBoxIcon.Error);
                Close();
            }
        }

        private static string BuildForwardedArguments()
        {
            string[] args = Environment.GetCommandLineArgs().Skip(1).ToArray();
            return string.Join(" ", args.Select(QuoteArgument));
        }

        private static string QuoteArgument(string argument)
        {
            if (string.IsNullOrEmpty(argument))
            {
                return "\"\"";
            }

            if (!argument.Any(char.IsWhiteSpace) && !argument.Contains("\""))
            {
                return argument;
            }

            return "\"" + argument.Replace("\\", "\\\\").Replace("\"", "\\\"") + "\"";
        }
    }
}
